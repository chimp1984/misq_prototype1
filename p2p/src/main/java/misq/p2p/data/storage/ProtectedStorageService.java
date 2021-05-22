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
import misq.p2p.NetworkData;
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

import static java.io.File.separator;

@Slf4j
public class ProtectedStorageService {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final int MAX_MAP_SIZE = 10000;

    // Max size of serialized NetworkData or MailboxMessage. Used to limit response map.
    // Depends on data types max. expected size.
    // Does not contain meta data like signatures and keys as well not the overhead from encryption.
    // So this number has to be fine tuned with real data later...
    private static final int MAX_INVENTORY_MAP_SIZE = 1_000_000;

    public interface Listener {
        void onAdded(ProtectedData protectedData);

        void onRemoved(ProtectedData protectedData);

        default void onRefreshed(ProtectedData protectedData) {
        }
    }

    private final String storageFilePath;
    private final MetaData metaData;
    final ConcurrentHashMap<MapKey, DataTransaction> map = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ProtectedStorageService(String storageDirPath, MetaData metaData) {
        this.storageFilePath = storageDirPath + separator + metaData.getFileName();
        this.metaData = metaData;

        if (new File(storageDirPath).exists()) {
            Serializable serializable = Persistence.read(storageDirPath);
            if (serializable instanceof ConcurrentHashMap) {
                ConcurrentHashMap<MapKey, DataTransaction> persisted = (ConcurrentHashMap<MapKey, DataTransaction>) serializable;
                maybePruneMap(persisted);
            }
        }
    }

    public AddProtectedDataRequest.Result add(AddProtectedDataRequest request) {
        ProtectedEntry entry = request.getEntry();
        ProtectedData protectedData = entry.getProtectedData();
        NetworkData networkData = protectedData.getNetworkData();

        MapKey mapKey;
        try {
            mapKey = getMapKey(networkData);
        } catch (NoSuchAlgorithmException e) {
            return new AddProtectedDataRequest.Result(false).cannotCreateHash(e);
        }
        DataTransaction dataTransaction = map.get(mapKey);
        int sequenceNumberFromMap = dataTransaction != null ? dataTransaction.getSequenceNumber() : 0;

        if (dataTransaction != null && entry.isSequenceNrInvalid(sequenceNumberFromMap)) {
            return new AddProtectedDataRequest.Result(false).sequenceNrInvalid();
        }

        if (entry.isExpired()) {
            return new AddProtectedDataRequest.Result(false).expired();
        }

        if (networkData.isDataInvalid()) {
            return new AddProtectedDataRequest.Result(false).dataInvalid();
        }

        if (request.isPublicKeyInvalid()) {
            return new AddProtectedDataRequest.Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new AddProtectedDataRequest.Result(false).signatureInvalid();
        }

        map.put(mapKey, request);
        listeners.forEach(listener -> listener.onAdded(protectedData));
        persist();
        return new AddProtectedDataRequest.Result(true);
    }

    public DataRequestResult remove(RemoveProtectedDataRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        DataTransaction requestFromMap = map.get(mapKey);

        if (requestFromMap == null) {
            // We don't have any entry but it might be that we would receive later an add request, so we need to keep
            // track of the sequence number
            map.put(mapKey, request);
            persist();
            return new DataRequestResult(false).noEntry();
        }

        if (requestFromMap instanceof RemoveProtectedDataRequest) {
            // We have had the entry already removed.
            if (request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                // We update the request so we have latest sequence number.
                map.put(mapKey, request);
                persist();
            }
            return new DataRequestResult(false).alreadyRemoved();
        }

        // At that point we know requestFromMap is an AddProtectedDataRequest
        AddProtectedDataRequest addProtectedDataRequest = (AddProtectedDataRequest) requestFromMap;
        // We have an entry, lets validate if we can remove it
        ProtectedEntry protectedEntryFromMap = addProtectedDataRequest.getEntry();
        ProtectedData dataFromMap = protectedEntryFromMap.getProtectedData();
        if (request.isSequenceNrInvalid(protectedEntryFromMap.getSequenceNumber())) {
            // Sequence number has not increased
            return new DataRequestResult(false).sequenceNrInvalid();
        }

        if (request.isPublicKeyInvalid(dataFromMap)) {
            // Hash of publicKey of data does not match provided one
            return new DataRequestResult(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new DataRequestResult(false).signatureInvalid();
        }

        map.put(mapKey, request);
        listeners.forEach(listener -> listener.onRemoved(dataFromMap));
        persist();
        return new DataRequestResult(true);
    }

    public DataRequestResult refresh(RefreshProtectedDataRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        DataTransaction requestFromMap = map.get(mapKey);

        if (requestFromMap == null) {
            return new DataRequestResult(false).noEntry();
        }

        if (requestFromMap instanceof RemoveProtectedDataRequest) {
            return new DataRequestResult(false).alreadyRemoved();
        }

        // At that point we know requestFromMap is an AddProtectedDataRequest
        AddProtectedDataRequest addRequestFromMap = (AddProtectedDataRequest) requestFromMap;
        // We have an entry, lets validate if we can remove it
        ProtectedEntry entryFromMap = addRequestFromMap.getEntry();
        ProtectedData dataFromMap = entryFromMap.getProtectedData();
        int sequenceNumberFromMap = entryFromMap.getSequenceNumber();
        if (request.isSequenceNrInvalid(sequenceNumberFromMap)) {
            // Sequence number has not increased
            return new DataRequestResult(false).sequenceNrInvalid();
        }

        if (request.isPublicKeyInvalid(dataFromMap)) {
            // Hash of publicKey of data does not match provided one
            return new DataRequestResult(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new DataRequestResult(false).signatureInvalid();
        }

        // Update request with new sequence number
        ProtectedEntry updatedEntryFromMap = new ProtectedEntry(dataFromMap,
                request.getSequenceNumber(),
                entryFromMap.getCreated());
        AddProtectedDataRequest updatedRequest = new AddProtectedDataRequest(updatedEntryFromMap,
                addRequestFromMap.getSignature(),
                addRequestFromMap.getOwnerPublicKey());
        map.put(mapKey, updatedRequest);
        listeners.forEach(listener -> listener.onRefreshed(dataFromMap));
        persist();
        return new DataRequestResult(true);
    }

    public Inventory getInventory(ProtectedDataFilter dataFilter) {
        return new Inventory(getInventoryMap(map, dataFilter.getFilterMap()));
    }

    Set<DataTransaction> getInventoryMap(ConcurrentHashMap<MapKey, DataTransaction> map,
                                         Map<MapKey, Integer> requesterMap) {
        //MAX_INVENTORY_MAP_SIZE
        return map.entrySet().stream()
                .filter(entry -> {
                    // Any entry we have but is not included in filter gets added
                    if (!requesterMap.containsKey(entry.getKey())) {
                        return true;
                    }
                    // If there is a match we add entry if sequence number is higher
                    return entry.getValue().getSequenceNumber() > requesterMap.get(entry.getKey());
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
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

    // todo call by time interval
    private void maybePruneMap(ConcurrentHashMap<MapKey, DataTransaction> current) {
        long now = System.currentTimeMillis();
        // Remove entries older than MAX_AGE
        // Remove expired ProtectedEntry in case value is of type AddProtectedDataRequest
        // Sort by created date
        // Limit to MAX_MAP_SIZE
        Map<MapKey, DataTransaction> pruned = current.entrySet().stream()
                .filter(entry -> now - entry.getValue().getCreated() < MAX_AGE)
                .filter(entry -> entry.getValue() instanceof RemoveProtectedDataRequest ||
                        !((AddProtectedDataRequest) entry.getValue()).getEntry().isExpired())
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (pruned.size() != current.size()) {
            map.putAll(pruned);
        }
    }
}
