/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.p2p.data.storage;

import lombok.extern.slf4j.Slf4j;
import misq.common.persistence.Persistence;
import misq.common.security.DigestUtil;
import misq.p2p.Proto;

import java.io.File;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class ProtectedStorageService {
    public interface Listener {
        void onAdded(ProtectedData protectedData);

        void onRemoved(ProtectedData protectedData);
    }

    private final String storageFilePath;
    final ConcurrentHashMap<MapKey, MapValue> map = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ProtectedStorageService(String storageFilePath) {
        this.storageFilePath = storageFilePath;

        if (new File(storageFilePath).exists()) {
            Serializable serializable = Persistence.read(storageFilePath);
            if (serializable instanceof ConcurrentHashMap) {
                ConcurrentHashMap<MapKey, MapValue> persisted = (ConcurrentHashMap<MapKey, MapValue>) serializable;
                map.putAll(persisted);
            }
        }
    }

    public AddProtectedDataRequest.Result add(AddProtectedDataRequest request) {
        ProtectedEntry entry = request.getEntry();
        ProtectedData protectedData = entry.getProtectedData();

        MapKey mapKey;
        try {
            mapKey = getMapKey(protectedData.getNetworkData());
        } catch (NoSuchAlgorithmException e) {
            return new AddProtectedDataRequest.Result(false).cannotCreateHash(e);
        }
        log.error("Add {}", mapKey);
        MapValue fromMap = map.get(mapKey);
        int sequenceNumberFromMap = fromMap != null ? fromMap.getSequenceNumber() : 0;

        // We do that check early as it is a very common case for returning, so we return early
        // If we have seen a more recent operation for this data and we have a data locally, ignore it
        if (fromMap != null && entry.isSequenceNrInvalid(sequenceNumberFromMap)) {
            return new AddProtectedDataRequest.Result(false).sequenceNrInvalid();
        }

        if (entry.isExpired()) {
            return new AddProtectedDataRequest.Result(false).expired();
        }

        if (protectedData.getNetworkData().isDataInvalid()) {
            return new AddProtectedDataRequest.Result(false).dataInvalid();
        }

        if (request.isPublicKeyInvalid()) {
            return new AddProtectedDataRequest.Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new AddProtectedDataRequest.Result(false).signatureInvalid();
        }

        map.put(mapKey, entry);
        listeners.forEach(listener -> listener.onAdded(protectedData));
        Persistence.write(map, storageFilePath);
        return new AddProtectedDataRequest.Result(true);
    }

    public RemoveProtectedDataRequest.Result remove(RemoveProtectedDataRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        log.error("Remove {}", mapKey);
        MapValue fromMap = map.get(mapKey);
        if (fromMap == null) {
            // We don't have the entry but it might be that we would receive later an add request, so we need to keep
            // track of the sequence number
            map.put(mapKey, new SequenceNumber(request.getSequenceNumber()));
            Persistence.write(map, storageFilePath);
            return new RemoveProtectedDataRequest.Result(false).noEntry();
        }

        if (fromMap instanceof SequenceNumber) {
            // We have had the entry already removed.
            return new RemoveProtectedDataRequest.Result(false).alreadyRemoved();
        }

        // We have an entry, lets validate if we can remove it
        ProtectedEntry protectedEntryFromMap = (ProtectedEntry) fromMap;
        ProtectedData dataFromMap = protectedEntryFromMap.getProtectedData();
        if (request.isSequenceNrInvalid(protectedEntryFromMap.getSequenceNumber())) {
            // Sequence number has not increased
            return new RemoveProtectedDataRequest.Result(false).sequenceNrInvalid();
        }

        if (request.isPublicKeyInvalid(dataFromMap)) {
            // Hash of publicKey of data does not match provided one
            return new RemoveProtectedDataRequest.Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new RemoveProtectedDataRequest.Result(false).signatureInvalid();
        }

        map.put(mapKey, new SequenceNumber(request.getSequenceNumber()));
        listeners.forEach(listener -> listener.onRemoved(dataFromMap));
        Persistence.write(map, storageFilePath);
        return new RemoveProtectedDataRequest.Result(true);
    }

    MapKey getMapKey(Proto data) throws NoSuchAlgorithmException {
        return new MapKey(DigestUtil.sha256(data.serialize()));
    }

    int getSequenceNumber(Proto data) throws NoSuchAlgorithmException {
        MapKey mapKey = getMapKey(data);
        if (map.containsKey(mapKey)) {
            return map.get(mapKey).getSequenceNumber();
        }
        return 0;
    }

    public void add(Listener listener) {
        listeners.add(listener);
    }

    public void remove(Listener listener) {
        listeners.remove(listener);
    }
}
