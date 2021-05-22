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
import misq.p2p.data.filter.ProtectedDataFilter;
import misq.p2p.data.inventory.Inventory;

import java.io.File;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ProtectedStorageService {
    private static final long MAX_AGE_SEQ_NUM = TimeUnit.DAYS.toMillis(10);
    private static final int MAX_MAP_SIZE = 10000;

    public interface Listener {
        void onAdded(ProtectedData protectedData);

        void onRemoved(ProtectedData protectedData);

        default void onRefreshed(ProtectedData protectedData) {
        }
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
                long now = System.currentTimeMillis();
                // Remove SequenceNumber entries older than MAX_AGE_SEQ_NUM
                // Remove expired ProtectedEntry
                // Sort by created date
                // Limit to MAX_MAP_SIZE
                Map<MapKey, MapValue> pruned = persisted.entrySet().stream()
                        .filter(entry -> {
                            if (entry.getValue() instanceof SequenceNumber) {
                                SequenceNumber sequenceNumber = (SequenceNumber) entry.getValue();
                                return now - sequenceNumber.getCreated() <= MAX_AGE_SEQ_NUM;
                            } else if (entry.getValue() instanceof ProtectedEntry) {
                                ProtectedEntry protectedEntry = (ProtectedEntry) entry.getValue();
                                return !protectedEntry.isExpired();
                            }
                            return true;
                        })
                        .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                        .limit(MAX_MAP_SIZE)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                map.putAll(pruned);
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
        MapValue mapValue = map.get(mapKey);
        int sequenceNumberFromMap = mapValue != null ? mapValue.getSequenceNumber() : 0;

        // We do that check early as it is a very common case for returning, so we return early
        // If we have seen a more recent operation for this data and we have a data locally, ignore it
        if (mapValue != null && entry.isSequenceNrInvalid(sequenceNumberFromMap)) {
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
        persist();
        return new AddProtectedDataRequest.Result(true);
    }

    public RemoveProtectedDataRequest.Result remove(RemoveProtectedDataRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        MapValue mapValue = map.get(mapKey);
        if (mapValue == null) {
            // We don't have the entry but it might be that we would receive later an add request, so we need to keep
            // track of the sequence number
            map.put(mapKey, new SequenceNumber(request.getSequenceNumber()));
            persist();
            return new RemoveProtectedDataRequest.Result(false).noEntry();
        }

        if (mapValue instanceof SequenceNumber) {
            // We have had the entry already removed.
            return new RemoveProtectedDataRequest.Result(false).alreadyRemoved();
        }

        // We have an entry, lets validate if we can remove it
        ProtectedEntry protectedEntryFromMap = (ProtectedEntry) mapValue;
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
        persist();
        return new RemoveProtectedDataRequest.Result(true);
    }

    public RefreshProtectedDataRequest.Result refresh(RefreshProtectedDataRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        MapValue mapValue = map.get(mapKey);
        if (mapValue == null) {
            // We don't have the entry but it might be that we would receive later an add request, so we need to keep
            // track of the sequence number
            map.put(mapKey, new SequenceNumber(request.getSequenceNumber()));
            persist();
            return new RefreshProtectedDataRequest.Result(false).noEntry();
        }

        if (mapValue instanceof SequenceNumber) {
            // We have had the entry already removed.
            return new RefreshProtectedDataRequest.Result(false).alreadyRemoved();
        }

        // We have an entry, lets validate if we can remove it
        ProtectedEntry protectedEntryFromMap = (ProtectedEntry) mapValue;
        if (request.isSequenceNrInvalid(protectedEntryFromMap.getSequenceNumber())) {
            // Sequence number has not increased
            return new RefreshProtectedDataRequest.Result(false).sequenceNrInvalid();
        }

        ProtectedData dataFromMap = protectedEntryFromMap.getProtectedData();
        if (request.isPublicKeyInvalid(dataFromMap)) {
            // Hash of publicKey of data does not match provided one
            return new RefreshProtectedDataRequest.Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new RefreshProtectedDataRequest.Result(false).signatureInvalid();
        }

        ProtectedEntry updated = new ProtectedEntry(dataFromMap, request.getSequenceNumber(), System.currentTimeMillis());
        map.put(mapKey, updated);
        listeners.forEach(listener -> listener.onRefreshed(dataFromMap));
        persist();
        return new RefreshProtectedDataRequest.Result(true);
    }

    public Inventory getInventory(ProtectedDataFilter dataFilter) {
        dataFilter.process(map);
        Set<MapValue> collect = map.entrySet().stream()
                .filter(e -> dataFilter.matches(e.getKey(), e.getValue()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        return new Inventory(collect);
    }

    private void persist() {
        Persistence.write(map, storageFilePath);
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
