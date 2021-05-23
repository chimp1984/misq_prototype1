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
import misq.common.security.DigestUtil;
import misq.common.security.KeyPairGeneratorUtil;
import misq.common.util.ObjectSerializer;
import misq.common.util.OsUtils;
import misq.p2p.NetworkData;
import misq.p2p.data.filter.ProtectedDataFilter;
import misq.p2p.data.filter.ProtectedDataFilterItem;
import org.junit.Test;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

@Slf4j
public class StorageTest {
    private String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testGetInv() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();
        MockNetworkData dummy = new MockNetworkData("");
        ProtectedStorageService service = storage.getService(dummy.getMetaData());
        int initialSeqNumFirstItem = 0;
        MockNetworkData first = null;
        byte[] hashOfFirst = new byte[]{};
        int iterations = 1;
        long ts = System.currentTimeMillis();
        int initialMapSize = service.map.size();
        log.error("initialMapSize={}", initialMapSize);
        for (int i = 0; i < iterations; i++) {
            // MockNetworkData mockNetworkData = new MockNetworkData("test" + i);
            MockNetworkData mockNetworkData = new MockNetworkData("test" + UUID.randomUUID().toString());
            AddProtectedDataRequest addRequest = storage.getAddProtectedDataRequest(mockNetworkData, keyPair); //2
            AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(addRequest); //6
            assertTrue(result.isSuccess());

            if (i == 0) {
                first = mockNetworkData;
                hashOfFirst = addRequest.getEntry().getProtectedData().getHashOfNetworkData();
                initialSeqNumFirstItem = service.getSequenceNumber(hashOfFirst);
            }
        }
        log.error("post map={}", service.map.size()); //took 5038 ms  100 initialMapSize=2111 / took 682 ms  for new map / took 1188 ms  300 init map
        //assertEquals(initialMapSize + iterations, service.map.size());
        log.error("took {} ms", System.currentTimeMillis() - ts);

        // request inventory with first item and same seq num
        // We should get iterations-1 items
        String dataType = dummy.getMetaData().getFileName();
        Set<ProtectedDataFilterItem> filterItems = new HashSet<>();
        filterItems.add(new ProtectedDataFilterItem(new MapKey(hashOfFirst).getHash(), initialSeqNumFirstItem + 1));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        int maxItems = service.getMaxItems();
        int expectedSize = Math.min(maxItems, service.map.size() - 1);
        int expectedTruncated = Math.max(0, service.map.size() - maxItems - 1);
        log.error("map={}, maxItems={}, iterations={}, maxItems={}, expectedSize {}, expectedTruncated={}",
                service.map.size(), maxItems, iterations, maxItems, expectedSize, expectedTruncated);
        log.error("dummy size={}", ObjectSerializer.serialize(dummy).length); // 251
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(expectedSize, inventory.getResult().size());
            assertEquals(expectedTruncated, inventory.getTruncated());

            log.error("inventory size={}", ObjectSerializer.serialize(inventory).length); //inventory size=238601 for 333 items. 716 bytes per item
            // map with 1440 items: file: 1.068.599 bytes, inventory size=1000517 ,  maxItems=1400
        });

    }

    //   @Test
    public void testAddAndRemove() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        MockNetworkData mockNetworkData = new MockNetworkData("test" + UUID.randomUUID().toString());
        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();

        AddProtectedDataRequest addRequest = storage.getAddProtectedDataRequest(mockNetworkData, keyPair);
        ProtectedStorageService service = storage.protectedStorageServices.get(addRequest.getFileName());
        int initialMapSize = service.map.size();
        byte[] hash = DigestUtil.sha256(mockNetworkData.serialize());
        int initialSeqNum = service.getSequenceNumber(hash);
        AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(addRequest);
        assertTrue(result.isSuccess());

        ConcurrentHashMap<MapKey, DataTransaction> map = service.map;
        MapKey mapKey = new MapKey(hash);
        AddProtectedDataRequest addRequestFromMap = (AddProtectedDataRequest) map.get(mapKey);
        ProtectedEntry entryFromMap = addRequestFromMap.getEntry();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());
        ProtectedData protectedData = addRequest.getEntry().getProtectedData();
        assertEquals(entryFromMap.getProtectedData(), protectedData);

        // request inventory with old seqNum
        String dataType = mockNetworkData.getMetaData().getFileName();
        Set<ProtectedDataFilterItem> filterItems = new HashSet<>();
        filterItems.add(new ProtectedDataFilterItem(mapKey.getHash(), initialSeqNum));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize + 1, inventory.getResult().size());
        });

        // request inventory with new seqNum
        filterItems = new HashSet<>();
        filterItems.add(new ProtectedDataFilterItem(mapKey.getHash(), initialSeqNum + 1));
        filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize, inventory.getResult().size());
        });

        // refresh
        RefreshProtectedDataRequest refreshRequest = storage.getRefreshProtectedDataRequest(mockNetworkData, keyPair);
        DataRequestResult refreshResult = storage.refreshProtectedStorageEntry(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddProtectedDataRequest) map.get(mapKey);
        entryFromMap = addRequestFromMap.getEntry();
        assertEquals(initialSeqNum + 2, entryFromMap.getSequenceNumber());

        //remove
        RemoveProtectedDataRequest removeRequest = storage.getRemoveProtectedDataRequest(mockNetworkData, keyPair);
        DataRequestResult removeDataResult = storage.removeProtectedStorageEntry(removeRequest);
        assertTrue(removeDataResult.isSuccess());

        RemoveProtectedDataRequest removeRequestFromMap = (RemoveProtectedDataRequest) map.get(mapKey);
        assertEquals(initialSeqNum + 3, removeRequestFromMap.getSequenceNumber());

        // refresh on removed fails
        RefreshProtectedDataRequest refreshAfterRemoveRequest = storage.getRefreshProtectedDataRequest(mockNetworkData, keyPair);
        DataRequestResult refreshAfterRemoveResult = storage.refreshProtectedStorageEntry(refreshAfterRemoveRequest);
        assertFalse(refreshAfterRemoveResult.isSuccess());

        // request inventory with old seqNum
        filterItems = new HashSet<>();
        filterItems.add(new ProtectedDataFilterItem(mapKey.getHash(), initialSeqNum + 2));
        filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize + 1, inventory.getResult().size());
        });

        // request inventory with new seqNum
        filterItems = new HashSet<>();
        filterItems.add(new ProtectedDataFilterItem(mapKey.getHash(), initialSeqNum + 3));
        filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize, inventory.getResult().size());
        });
    }

    //  @Test
    public void testAddAndRemoveMailboxMsg() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        KeyPair senderKeyPair = KeyPairGeneratorUtil.generateKeyPair();
        KeyPair receiverKeyPair = KeyPairGeneratorUtil.generateKeyPair();

        MockMailboxMessage mockMailboxMessage = new MockMailboxMessage("test" + UUID.randomUUID().toString());
        SealedData sealedData = storage.getSealedData(mockMailboxMessage, senderKeyPair, receiverKeyPair.getPublic());
        ProtectedStorageService service = storage.getService(sealedData.getMetaData());
        ConcurrentHashMap<MapKey, DataTransaction> map = service.map;
        int initialMapSize = map.size();
        byte[] hash = DigestUtil.sha256(sealedData.serialize());
        int initialSeqNum = service.getSequenceNumber(hash);
        AddMailboxDataRequest request = storage.getAddMailboxDataRequest(sealedData, senderKeyPair, receiverKeyPair.getPublic());
        AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(request);
        assertTrue(result.isSuccess());

        MapKey mapKey = new MapKey(hash);
        AddProtectedDataRequest addRequestFromMap = (AddProtectedDataRequest) map.get(mapKey);
        ProtectedEntry entryFromMap = addRequestFromMap.getEntry();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());

        assertTrue(entryFromMap instanceof MailboxEntry);
        MailboxEntry mailboxEntryFromMap = (MailboxEntry) entryFromMap;
        NetworkData sealedDataFromMap = mailboxEntryFromMap.getProtectedData().getNetworkData();
        assertEquals(sealedDataFromMap, sealedData);

        // request inventory with old seqNum
        String dataType = sealedData.getMetaData().getFileName();
        Set<ProtectedDataFilterItem> filterItems = new HashSet<>();
        filterItems.add(new ProtectedDataFilterItem(mapKey.getHash(), initialSeqNum));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize + 1, inventory.getResult().size());
        });


        // refresh
        RefreshProtectedDataRequest refreshRequest = storage.getRefreshProtectedDataRequest(sealedData, senderKeyPair);
        DataRequestResult refreshResult = storage.refreshProtectedStorageEntry(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddProtectedDataRequest) map.get(mapKey);
        entryFromMap = addRequestFromMap.getEntry();
        assertEquals(initialSeqNum + 2, entryFromMap.getSequenceNumber());

        // remove
        RemoveMailboxDataRequest removeMailboxDataRequest = storage.getRemoveMailboxDataRequest(sealedData, receiverKeyPair);

        DataRequestResult removeDataResult = storage.removeProtectedStorageEntry(removeMailboxDataRequest);
        assertTrue(removeDataResult.isSuccess());

        RemoveProtectedDataRequest removeRequestFromMap = (RemoveProtectedDataRequest) map.get(mapKey);
        assertEquals(Integer.MAX_VALUE, removeRequestFromMap.getSequenceNumber());

        // we must not create a new sealed data as it would have a diff. secret key and so a diff hash...
        // If users re-publish mailbox messages they need to keep the original sealed data and re-use that instead
        // of creating new ones, as otherwise it would appear like a new mailbox msg.
        assertFalse(storage.canAddMailboxMessage(sealedData));
        try {
            // calling getAddMailboxDataRequest without the previous canAddMailboxMessage check will throw
            storage.getAddMailboxDataRequest(sealedData, senderKeyPair, receiverKeyPair.getPublic());
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

        // using the old request again would fail as seq number is not allowing it
        AddProtectedDataRequest.Result result2 = storage.addProtectedStorageEntry(request);
        assertFalse(result2.isSuccess());
        assertTrue(result2.isSequenceNrInvalid());

        // refresh on removed fails
        RefreshProtectedDataRequest refreshRequest2 = storage.getRefreshProtectedDataRequest(sealedData, senderKeyPair);
        DataRequestResult refreshResult2 = storage.refreshProtectedStorageEntry(refreshRequest);
        assertFalse(refreshResult2.isSuccess());
    }
}
