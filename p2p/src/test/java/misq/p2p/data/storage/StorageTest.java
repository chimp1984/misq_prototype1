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
import misq.common.security.KeyPairGeneratorUtil;
import misq.common.util.OsUtils;
import misq.p2p.NetworkData;
import org.junit.Test;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

@Slf4j
public class StorageTest {
    private String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testAddAndRemove() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        MockNetworkData mockNetworkData = new MockNetworkData("test");
        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();

        AddProtectedDataRequest addRequest = storage.getAddProtectedDataRequest(mockNetworkData, keyPair);
        ProtectedStorageService service = storage.storageServices.get(addRequest.getFileName());
        int initialSeqNum = service.getSequenceNumber(mockNetworkData);
        AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(addRequest);
        assertTrue(result.isSuccess());

        ConcurrentHashMap<MapKey, DataTransaction> map = service.map;
        MapKey mapKey = service.getMapKey(mockNetworkData);
        AddProtectedDataRequest addRequestFromMap = (AddProtectedDataRequest) map.get(mapKey);
        ProtectedEntry entryFromMap = addRequestFromMap.getEntry();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());
        ProtectedData protectedData = addRequest.getEntry().getProtectedData();
        assertEquals(entryFromMap.getProtectedData(), protectedData);

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
    }

    @Test
    public void testAddAndRemoveMailboxMsg() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        KeyPair senderKeyPair = KeyPairGeneratorUtil.generateKeyPair();
        KeyPair receiverKeyPair = KeyPairGeneratorUtil.generateKeyPair();

        MockMailboxMessage mockMailboxMessage = new MockMailboxMessage("test");
        SealedData sealedData = storage.getSealedData(mockMailboxMessage, senderKeyPair, receiverKeyPair.getPublic());
        ProtectedStorageService service = storage.getService(sealedData.getMetaData());
        ConcurrentHashMap<MapKey, DataTransaction> map = service.map;

        int initialSeqNum = service.getSequenceNumber(sealedData);
        AddMailboxDataRequest request = storage.getAddMailboxDataRequest(sealedData, senderKeyPair, receiverKeyPair.getPublic());
        AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(request);
        assertTrue(result.isSuccess());

        MapKey mapKey = service.getMapKey(sealedData);
        AddProtectedDataRequest addRequestFromMap = (AddProtectedDataRequest) map.get(mapKey);
        ProtectedEntry entryFromMap = addRequestFromMap.getEntry();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());

        assertTrue(entryFromMap instanceof MailboxEntry);
        MailboxEntry mailboxEntryFromMap = (MailboxEntry) entryFromMap;
        NetworkData sealedDataFromMap = mailboxEntryFromMap.getProtectedData().getNetworkData();
        assertEquals(sealedDataFromMap, sealedData);

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
