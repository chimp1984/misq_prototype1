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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class StorageTest {
    private String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testAddAndRemove() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        MockNetworkData mockNetworkData = new MockNetworkData("test");
        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();

        AddProtectedDataRequest request = storage.getAddProtectedDataRequest(mockNetworkData, keyPair);
        ProtectedStorageService service = storage.storageServices.get(request.getFileName());
        int seqNrBefore = service.getSequenceNumber(mockNetworkData);
        AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(request);
        assertTrue(result.isSuccess());

        MapValue mapValue1 = service.map.get(service.getMapKey(mockNetworkData));
        assertEquals(seqNrBefore + 1, mapValue1.getSequenceNumber());
        ProtectedData protectedData = request.getEntry().getProtectedData();
        MapKey mapKey = service.getMapKey(protectedData.getNetworkData());
        MapValue mapValue = service.map.get(mapKey);
        assertTrue(mapValue instanceof ProtectedEntry);
        ProtectedEntry protectedEntry = (ProtectedEntry) mapValue;
        assertEquals(protectedEntry.getProtectedData(), protectedData);

        RemoveProtectedDataRequest removeProtectedDataRequest = storage.getRemoveProtectedDataRequest(mockNetworkData, keyPair);

        RemoveProtectedDataRequest.Result removeDataResult = storage.removeProtectedStorageEntry(removeProtectedDataRequest);
        log.error(removeDataResult.toString());
        assertTrue(removeDataResult.isSuccess());

        mapValue = service.map.get(mapKey);
        assertTrue(mapValue instanceof SequenceNumber);
        SequenceNumber sequenceNumber = (SequenceNumber) mapValue;
        assertEquals(seqNrBefore + 2, sequenceNumber.getSequenceNumber());
    }

    @Test
    public void testAddAndRemoveMailboxMsg() throws GeneralSecurityException {
        //todo with new keypair we get new entries each run...
        Storage storage = new Storage(appDirPath);
        KeyPair senderKeyPair = KeyPairGeneratorUtil.generateKeyPair();
        KeyPair receiverKeyPair = KeyPairGeneratorUtil.generateKeyPair();

        MockMailboxMessage mockMailboxMessage = new MockMailboxMessage("test");
        AddMailboxDataRequest request = storage.getAddMailboxDataRequest(mockMailboxMessage, senderKeyPair, receiverKeyPair.getPublic());
        ProtectedStorageService service = storage.storageServices.get(request.getFileName());
        int seqNrBefore = service.getSequenceNumber(mockMailboxMessage);
        AddProtectedDataRequest.Result result = storage.addProtectedStorageEntry(request);
        log.error(result.toString());
        assertTrue(result.isSuccess());


        MapValue mapValue1 = service.map.get(service.getMapKey(mockMailboxMessage));
        //   assertEquals(seqNrBefore + 1, mapValue1.getSequenceNumber());

        SealedData sealedData = (SealedData) request.getEntry().getProtectedData().getNetworkData();
        MapKey mapKey = service.getMapKey(sealedData);
        MapValue mapValue = service.map.get(mapKey);
        assertTrue(mapValue instanceof MailboxEntry);
        MailboxEntry protectedPayloadEntry = (MailboxEntry) mapValue;
        NetworkData sealedDataPayload2 = protectedPayloadEntry.getProtectedData().getNetworkData();
        assertEquals(sealedDataPayload2, sealedData);

        String fileName = mockMailboxMessage.getClass().getSimpleName();
        RemoveMailboxDataRequest removeMailboxDataRequest = storage.getRemoveMailboxDataRequest(fileName, sealedData, receiverKeyPair);

        RemoveProtectedDataRequest.Result removeDataResult = storage.removeProtectedStorageEntry(removeMailboxDataRequest);
        log.error(removeDataResult.toString());
        assertTrue(removeDataResult.isSuccess());
        mapValue = service.map.get(mapKey);
        assertTrue(mapValue instanceof SequenceNumber);
        SequenceNumber sequenceNumber = (SequenceNumber) mapValue;
        // assertEquals(seqNrBefore+2, sequenceNumber.getSequenceNumber());
    }
}