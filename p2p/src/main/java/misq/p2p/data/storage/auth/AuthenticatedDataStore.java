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

package misq.p2p.data.storage.auth;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import misq.common.persistence.Persistence;
import misq.common.security.DigestUtil;
import misq.p2p.data.filter.ProtectedDataFilter;
import misq.p2p.data.inventory.Inventory;
import misq.p2p.data.storage.MapKey;
import misq.p2p.data.storage.MetaData;
import misq.p2p.data.storage.mailbox.AddMailboxRequest;
import misq.p2p.data.storage.mailbox.MailboxData;
import misq.p2p.data.storage.mailbox.MailboxPayload;

import java.io.File;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.io.File.separator;

@Slf4j
public class AuthenticatedDataStore {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(10);
    private static final int MAX_MAP_SIZE = 10000;

    // Max size of serialized NetworkData or MailboxMessage. Used to limit response map.
    // Depends on data types max. expected size.
    // Does not contain meta data like signatures and keys as well not the overhead from encryption.
    // So this number has to be fine tuned with real data later...
    private static final int MAX_INVENTORY_MAP_SIZE = 1_000_000;

    public interface Listener {
        void onAdded(AuthenticatedPayload authenticatedPayload);

        void onRemoved(AuthenticatedPayload authenticatedPayload);

        default void onRefreshed(AuthenticatedPayload authenticatedPayload) {
        }
    }

    private final String storageFilePath;
    private final int maxItems;
    private final ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = new ConcurrentHashMap<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public AuthenticatedDataStore(String storageDirPath, MetaData metaData) {
        this.storageFilePath = storageDirPath + separator + metaData.getFileName();
        maxItems = MAX_INVENTORY_MAP_SIZE / metaData.getMaxSizeInBytes();

        if (new File(storageFilePath).exists()) {
            Serializable serializable = Persistence.read(storageFilePath);
            if (serializable instanceof ConcurrentHashMap) {
                ConcurrentHashMap<MapKey, AuthenticatedDataRequest> persisted = (ConcurrentHashMap<MapKey, AuthenticatedDataRequest>) serializable;
                maybePruneMap(persisted);
            }
        }
    }

    public Result add(AddRequest request) throws NoSuchAlgorithmException {
        AuthenticatedData entry = request.getAuthenticatedData();
        AuthenticatedPayload authenticatedPayload = entry.getAuthenticatedPayload();
        byte[] hash = DigestUtil.sha256(authenticatedPayload.serialize());
        MapKey mapKey = new MapKey(hash);
        AuthenticatedDataRequest dataRequest = map.get(mapKey);
        int sequenceNumberFromMap = dataRequest != null ? dataRequest.getSequenceNumber() : 0;

        if (dataRequest != null && entry.isSequenceNrInvalid(sequenceNumberFromMap)) {
            return new Result(false).sequenceNrInvalid();
        }

        if (entry.isExpired()) {
            return new Result(false).expired();
        }

        if (authenticatedPayload.isDataInvalid()) {
            return new Result(false).dataInvalid();
        }

        if (request.isPublicKeyInvalid()) {
            return new Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new Result(false).signatureInvalid();
        }

        map.put(mapKey, request);
        listeners.forEach(listener -> listener.onAdded(authenticatedPayload));
        persist();
        return new Result(true);
    }

    public Result remove(RemoveRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        AuthenticatedDataRequest requestFromMap = map.get(mapKey);

        if (requestFromMap == null) {
            // We don't have any entry but it might be that we would receive later an add request, so we need to keep
            // track of the sequence number
            map.put(mapKey, request);
            persist();
            return new Result(false).noEntry();
        }

        if (requestFromMap instanceof RemoveRequest) {
            // We have had the entry already removed.
            if (request.isSequenceNrInvalid(requestFromMap.getSequenceNumber())) {
                // We update the request so we have latest sequence number.
                map.put(mapKey, request);
                persist();
            }
            return new Result(false).alreadyRemoved();
        }

        // At that point we know requestFromMap is an AddProtectedDataRequest
        AddRequest addRequest = (AddRequest) requestFromMap;
        // We have an entry, lets validate if we can remove it
        AuthenticatedData authenticatedDataFromMap = addRequest.getAuthenticatedData();
        AuthenticatedPayload dataFromMap = authenticatedDataFromMap.getAuthenticatedPayload();
        if (request.isSequenceNrInvalid(authenticatedDataFromMap.getSequenceNumber())) {
            // Sequence number has not increased
            return new Result(false).sequenceNrInvalid();
        }

        if (request.isPublicKeyInvalid(authenticatedDataFromMap)) {
            // Hash of publicKey of data does not match provided one
            return new Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new Result(false).signatureInvalid();
        }

        map.put(mapKey, request);
        listeners.forEach(listener -> listener.onRemoved(dataFromMap));
        persist();
        return new Result(true);
    }

    public Result refresh(RefreshRequest request) {
        MapKey mapKey = new MapKey(request.getHash());
        AuthenticatedDataRequest requestFromMap = map.get(mapKey);

        if (requestFromMap == null) {
            return new Result(false).noEntry();
        }

        if (requestFromMap instanceof RemoveRequest) {
            return new Result(false).alreadyRemoved();
        }

        // At that point we know requestFromMap is an AddProtectedDataRequest
        AddRequest addRequestFromMap = (AddRequest) requestFromMap;
        // We have an entry, lets validate if we can remove it
        AuthenticatedData entryFromMap = addRequestFromMap.getAuthenticatedData();
        AuthenticatedPayload dataFromMap = entryFromMap.getAuthenticatedPayload();
        int sequenceNumberFromMap = entryFromMap.getSequenceNumber();
        if (request.isSequenceNrInvalid(sequenceNumberFromMap)) {
            // Sequence number has not increased
            return new Result(false).sequenceNrInvalid();
        }

        if (request.isPublicKeyInvalid(entryFromMap)) {
            // Hash of publicKey of data does not match provided one
            return new Result(false).publicKeyInvalid();
        }

        if (request.isSignatureInvalid()) {
            return new Result(false).signatureInvalid();
        }

        // Update request with new sequence number
        AddRequest updatedRequest;
        if (addRequestFromMap instanceof AddMailboxRequest) {
            MailboxData mailboxDataFromMap = (MailboxData) entryFromMap;
            MailboxPayload mailboxPayloadFromMap = (MailboxPayload) dataFromMap;
            MailboxData updatedEntryFromMap = new MailboxData(mailboxPayloadFromMap,
                    request.getSequenceNumber(),
                    mailboxDataFromMap.getHashOfPublicKey(),
                    mailboxDataFromMap.getHashOfReceiversPublicKey(),
                    mailboxDataFromMap.getReceiversPubKey(),
                    mailboxDataFromMap.getCreated());
            updatedRequest = new AddMailboxRequest(updatedEntryFromMap,
                    addRequestFromMap.getSignature(),
                    addRequestFromMap.getOwnerPublicKey());
        } else {
            AuthenticatedData updatedEntryFromMap = new AuthenticatedData(dataFromMap,
                    request.getSequenceNumber(),
                    entryFromMap.getHashOfPublicKey(),
                    entryFromMap.getCreated());
            updatedRequest = new AddRequest(updatedEntryFromMap,
                    addRequestFromMap.getSignature(),
                    addRequestFromMap.getOwnerPublicKey());
        }

        map.put(mapKey, updatedRequest);
        listeners.forEach(listener -> listener.onRefreshed(dataFromMap));
        persist();
        return new Result(true);
    }

    public Inventory getInventory(ProtectedDataFilter dataFilter) {
        List<AuthenticatedDataRequest> inventoryMap = getInventoryMap(map, dataFilter.getFilterMap());
        int maxItems = getMaxItems();
        int size = inventoryMap.size();
        if (size <= maxItems) {
            return new Inventory(inventoryMap, 0);
        }

        List<AuthenticatedDataRequest> result = getSubSet(inventoryMap, dataFilter.getOffset(), dataFilter.getRange(), maxItems);
        int numDropped = size - result.size();
        return new Inventory(result, numDropped);
    }

    @VisibleForTesting
    public static List<AuthenticatedDataRequest> getSubSet(List<AuthenticatedDataRequest> map, int filterOffset, int filterRange, int maxItems) {
        int size = map.size();
        checkArgument(filterOffset >= 0);
        checkArgument(filterOffset <= 100);
        checkArgument(filterRange >= 0);
        checkArgument(filterRange <= 100);
        checkArgument(filterOffset + filterRange <= 100);
        int offset = size * filterOffset / 100;
        int range = size * filterRange / 100;
        return map.stream()
                .sorted(Comparator.comparingLong(AuthenticatedDataRequest::getCreated))
                .skip(offset)
                .limit(range)
                .limit(maxItems)
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    public int getMaxItems() {
        return maxItems;
    }

    List<AuthenticatedDataRequest> getInventoryMap(ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map,
                                                   Map<MapKey, Integer> requesterMap) {
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
                .collect(Collectors.toList());
    }

    private void persist() {
        Persistence.write(map, storageFilePath);
    }

    public int getSequenceNumber(byte[] hash) {
        MapKey mapKey = new MapKey(hash);
        if (map.containsKey(mapKey)) {
            return map.get(mapKey).getSequenceNumber();
        }
        return 0;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // todo call by time interval
    private void maybePruneMap(ConcurrentHashMap<MapKey, AuthenticatedDataRequest> current) {
        long now = System.currentTimeMillis();
        // Remove entries older than MAX_AGE
        // Remove expired ProtectedEntry in case value is of type AddProtectedDataRequest
        // Sort by created date
        // Limit to MAX_MAP_SIZE
        Map<MapKey, AuthenticatedDataRequest> pruned = current.entrySet().stream()
                .filter(entry -> now - entry.getValue().getCreated() < MAX_AGE)
                .filter(entry -> entry.getValue() instanceof RemoveRequest ||
                        !((AddRequest) entry.getValue()).getAuthenticatedData().isExpired())
                .sorted((o1, o2) -> Long.compare(o2.getValue().getCreated(), o1.getValue().getCreated()))
                .limit(MAX_MAP_SIZE)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        map.putAll(pruned);
    }

    @VisibleForTesting
    public ConcurrentHashMap<MapKey, AuthenticatedDataRequest> getMap() {
        return map;
    }
}
