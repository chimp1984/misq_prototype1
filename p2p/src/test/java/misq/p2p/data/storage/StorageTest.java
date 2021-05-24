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
import misq.common.security.SignatureUtil;
import misq.common.util.Hex;
import misq.common.util.ObjectSerializer;
import misq.common.util.OsUtils;
import misq.p2p.data.NetworkData;
import misq.p2p.data.filter.FilterItem;
import misq.p2p.data.filter.ProtectedDataFilter;
import misq.p2p.data.storage.auth.*;
import misq.p2p.data.storage.auth.mailbox.AddMailboxRequest;
import misq.p2p.data.storage.auth.mailbox.Mailbox;
import misq.p2p.data.storage.auth.mailbox.MailboxPayload;
import misq.p2p.data.storage.auth.mailbox.RemoveMailboxRequest;
import org.junit.Test;

import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
        MockProtectedData dummy = new MockProtectedData("");
        AuthenticatedDataStore service = storage.getService(dummy.getMetaData());
        int initialSeqNumFirstItem = 0;
        MockProtectedData first = null;
        byte[] hashOfFirst = new byte[]{};
        int iterations = 1;
        long ts = System.currentTimeMillis();
        int initialMapSize = service.getMap().size();
        log.error("initialMapSize={}", initialMapSize);
        for (int i = 0; i < iterations; i++) {
            // MockNetworkData mockNetworkData = new MockNetworkData("test" + i);
            MockProtectedData mockProtectedData = new MockProtectedData("test" + UUID.randomUUID().toString());
            AddRequest addRequest = storage.getAddProtectedDataRequest(mockProtectedData, keyPair); //2
            Result result = storage.addProtectedStorageEntry(addRequest); //6
            assertTrue(result.isSuccess());

            if (i == 0) {
                first = mockProtectedData;
                hashOfFirst = DigestUtil.sha256(first.serialize());
                initialSeqNumFirstItem = service.getSequenceNumber(hashOfFirst);
            }
        }
        log.error("post map={}", service.getMap().size()); //took 5038 ms  100 initialMapSize=2111 / took 682 ms  for new map / took 1188 ms  300 init map
        //assertEquals(initialMapSize + iterations, service.getMap().size());
        log.error("took {} ms", System.currentTimeMillis() - ts);

        // request inventory with first item and same seq num
        // We should get iterations-1 items
        String dataType = dummy.getMetaData().getFileName();
        Set<FilterItem> filterItems = new HashSet<>();
        filterItems.add(new FilterItem(new MapKey(hashOfFirst).getHash(), initialSeqNumFirstItem + 1));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        int maxItems = service.getMaxItems();
        int expectedSize = Math.min(maxItems, service.getMap().size() - 1);
        int expectedTruncated = Math.max(0, service.getMap().size() - maxItems - 1);
        log.error("getMap()={}, maxItems={}, iterations={}, maxItems={}, expectedSize {}, expectedTruncated={}",
                service.getMap().size(), maxItems, iterations, maxItems, expectedSize, expectedTruncated);
        log.error("dummy size={}", ObjectSerializer.serialize(dummy).length); // 251
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(expectedSize, inventory.getEntries().size());
            assertEquals(expectedTruncated, inventory.getNumDropped());

            log.error("inventory size={}", ObjectSerializer.serialize(inventory).length); //inventory size=238601 for 333 items. 716 bytes per item
            // map with 1440 items: file: 1.068.599 bytes, inventory size=1000517 ,  maxItems=1400
        });

    }

    //SignedData
    @Test
    public void testSignedData() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);

        String privateKeyAsHex = "308204bd020100300d06092a864886f70d0101010500048204a7308204a30201000282010100b2020f519d4538bc7820b6f6134be47ea1b4cdfc11b8e3191caf2c62f9718c0bf538975c86a60a7a3444baa24fd23e85056d81c9f1aae429d316c02d21ac596d895a68574c2b0b7da7c437c598548f6d9d57e42917073c60fc0e2c8ab646df634a7fbde2561ba3f0bf773dca1adcde12fc210d57737d641b9e7a1d3a190857b2b3d1a7e7feadced0d36c2dd48f02096eb445bcd6b27bfa87bfc6310a6ff3a5bf546abda0946b56433c99feda10c68716a22e2c8c6201357dc5dc122c6edd118364d9fc62539c30dfd77cd08990a382da27ce672f228903ffce10000c92e8ef9d114d3cd578f7bccac644ec2f01477c839c3fe2dcd48ff0d79754ccf18d93efd902030100010282010052322d4cac6868586ec7fc74085904fb8c43e6d1bc853928415c8a0c71ce82b28adb44d94ebba11ee8bfcbc663415be924a67a45fbb7c37fa49f5ec1f848934ac71fde0a617b86e4d0f10427d3bd48357f703444dd0354581c4b9739be0d5fdd836b150d4c619b03f205679cc966af8ce8ab82739d3a8b5d4d8269b88812981b7e83af269a0c237234634b57c278fcad0f1735ab1bad3311d908e8bfaa29dcff319bfac9500678f3be1292bc0f32e96ccd52277e263b7aaa778d4f7bdb711650f99d53c32b5fbcef477093f676475189878798146c10f96eecc1ebe5b80449b207ce78a704663ed8a4a2437bcdb4b52909c6b240b9bd2067aff5bb33beef12f902818100fcb4ad42ea5d5076febc41a81211415a8d1a8f6f645343df0cec5ade7e71c1ed94b397be2637f81bb1973c724952c6c445c940218bbf53ed1e1c0ca20aa03cfd0116093fd02e80ffcef77ddbf4c63466ac4155cc474eec5d9c709a5b3a0972dfdc32aedb0ed65c735f19f02037a8034568f83261f5f6c67e6245797cb8585fd702818100b4541a8d2e020b7c4bc89c09735759e703ec9f29e0d7e709c74855f8e42de5074f1c3386f16cef3fcbca654b97bfe18d897546f6fa8488b518c530a831e103681f536d67501f668e90c9718bded929b231262ba09346a22a9c00679b8e6bd95565b0ca6661d455817c16c3f894759be0cccdf0cf7f7b406761d960f1f6caf7cf028181009cdce6841808ce8edef72ae65238c5d198af39041349a062cf99d39a32f118490aba2462534500cce80311f17b5457afb40605ba0d0e39e1818435cf4c3b454063b133129a7e9372b71d67d1e672364ad97840f2e9fcb2ba3506acba1e1f89602e4683c5d4c2f966604d30823f2a1ac5b63002ce4e28ddf3cbba867c05ce4dc702818021e9b3389bcf6ca38a8906b74c46c0348eeb601f7b167f6fba57a33b7486210d57d660e65edef2bb97b2cf8c00d4e8313b09a037f0731e56987af5249c84c9a43a47f14a3daa3a1a53a65ec1443ea8f5c7027bafee22997ad3edcb8e58a175b4f6b3e1cc915762614099f36efb5486e526ff0feba5f8e2eace5f1839490570c70281807c74da2b71789f5abbb087c3540514def5f6db7314a9b73b483a0b1db17ebe0779ad25fc1281c8d128a81c3d9a598615be4f11055afb0679d8f3ebd7a6573e644fd56a390ac61886c5f6ed960a1c7ad94e17e1c28650bf56bcf250ba074470daa29276bdb66b874b3bc14a3cc1fc28ec5457640142a9f4326a0034c5b913debd";
        byte[] privateKeyBytes = Hex.decode(privateKeyAsHex);
        PrivateKey privateKey = KeyPairGeneratorUtil.generatePrivate(privateKeyBytes);
        PublicKey publicKey = KeyPairGeneratorUtil.generatePublic(privateKey);
        AuthenticatedPayload networkData = new MockProtectedData("test" + UUID.randomUUID().toString());
        byte[] signature = SignatureUtil.sign(networkData.serialize(), privateKey);
        MockAuthorizedData mockAuthorizedData = new MockAuthorizedData(networkData, signature, publicKey);

        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();
        AddRequest addRequest = storage.getAddProtectedDataRequest(mockAuthorizedData, keyPair);
        AuthenticatedDataStore service = storage.protectedStorageServices.get(addRequest.getFileName());
        byte[] hash = DigestUtil.sha256(mockAuthorizedData.serialize());
        int initialSeqNum = service.getSequenceNumber(hash);
        Result result = storage.addProtectedStorageEntry(addRequest);
        log.error(result.toString());
        assertTrue(result.isSuccess());

        ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = service.getMap();
        MapKey mapKey = new MapKey(hash);
        AddRequest addRequestFromMap = (AddRequest) map.get(mapKey);
        AuthenticatedData entryFromMap = addRequestFromMap.getAuthenticatedData();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());
        AuthenticatedPayload authenticatedPayload = addRequest.getAuthenticatedData().getAuthenticatedPayload();
        assertEquals(entryFromMap.getAuthenticatedPayload(), authenticatedPayload);

        // refresh
        RefreshRequest refreshRequest = storage.getRefreshProtectedDataRequest(mockAuthorizedData, keyPair);
        Result refreshResult = storage.refreshProtectedStorageEntry(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddRequest) map.get(mapKey);
        entryFromMap = addRequestFromMap.getAuthenticatedData();
        assertEquals(initialSeqNum + 2, entryFromMap.getSequenceNumber());

        //remove
        RemoveRequest removeRequest = storage.getRemoveProtectedDataRequest(mockAuthorizedData, keyPair);
        Result removeDataResult = storage.removeProtectedStorageEntry(removeRequest);
        assertTrue(removeDataResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(initialSeqNum + 3, removeRequestFromMap.getSequenceNumber());
    }

    @Test
    public void testAddAndRemove() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        MockProtectedData mockProtectedData = new MockProtectedData("test" + UUID.randomUUID().toString());
        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();

        AddRequest addRequest = storage.getAddProtectedDataRequest(mockProtectedData, keyPair);
        AuthenticatedDataStore service = storage.protectedStorageServices.get(addRequest.getFileName());
        int initialMapSize = service.getMap().size();
        byte[] hash = DigestUtil.sha256(mockProtectedData.serialize());
        int initialSeqNum = service.getSequenceNumber(hash);
        Result result = storage.addProtectedStorageEntry(addRequest);
        assertTrue(result.isSuccess());

        ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = service.getMap();
        MapKey mapKey = new MapKey(hash);
        AddRequest addRequestFromMap = (AddRequest) map.get(mapKey);
        AuthenticatedData entryFromMap = addRequestFromMap.getAuthenticatedData();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());
        AuthenticatedPayload authenticatedPayload = addRequest.getAuthenticatedData().getAuthenticatedPayload();
        assertEquals(entryFromMap.getAuthenticatedPayload(), authenticatedPayload);

        // request inventory with old seqNum
        String dataType = mockProtectedData.getMetaData().getFileName();
        Set<FilterItem> filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize + 1, inventory.getEntries().size());
        });

        // request inventory with new seqNum
        filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum + 1));
        filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize, inventory.getEntries().size());
        });

        // refresh
        RefreshRequest refreshRequest = storage.getRefreshProtectedDataRequest(mockProtectedData, keyPair);
        Result refreshResult = storage.refreshProtectedStorageEntry(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddRequest) map.get(mapKey);
        entryFromMap = addRequestFromMap.getAuthenticatedData();
        assertEquals(initialSeqNum + 2, entryFromMap.getSequenceNumber());

        //remove
        RemoveRequest removeRequest = storage.getRemoveProtectedDataRequest(mockProtectedData, keyPair);
        Result removeDataResult = storage.removeProtectedStorageEntry(removeRequest);
        assertTrue(removeDataResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(initialSeqNum + 3, removeRequestFromMap.getSequenceNumber());

        // refresh on removed fails
        RefreshRequest refreshAfterRemoveRequest = storage.getRefreshProtectedDataRequest(mockProtectedData, keyPair);
        Result refreshAfterRemoveResult = storage.refreshProtectedStorageEntry(refreshAfterRemoveRequest);
        assertFalse(refreshAfterRemoveResult.isSuccess());

        // request inventory with old seqNum
        filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum + 2));
        filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize + 1, inventory.getEntries().size());
        });

        // request inventory with new seqNum
        filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum + 3));
        filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize, inventory.getEntries().size());
        });
    }

    @Test
    public void testAddAndRemoveMailboxMsg() throws GeneralSecurityException {
        Storage storage = new Storage(appDirPath);
        KeyPair senderKeyPair = KeyPairGeneratorUtil.generateKeyPair();
        KeyPair receiverKeyPair = KeyPairGeneratorUtil.generateKeyPair();

        MockMailboxMessage mockMailboxMessage = new MockMailboxMessage("test" + UUID.randomUUID().toString());
        MailboxPayload mailboxPayload = storage.getSealedData(mockMailboxMessage, senderKeyPair, receiverKeyPair.getPublic());
        AuthenticatedDataStore service = storage.getService(mailboxPayload.getMetaData());
        ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = service.getMap();
        int initialMapSize = map.size();
        byte[] hash = DigestUtil.sha256(mailboxPayload.serialize());
        int initialSeqNum = service.getSequenceNumber(hash);
        AddMailboxRequest request = storage.getAddMailboxDataRequest(mailboxPayload, senderKeyPair, receiverKeyPair.getPublic());
        Result result = storage.addProtectedStorageEntry(request);
        assertTrue(result.isSuccess());

        MapKey mapKey = new MapKey(hash);
        AddRequest addRequestFromMap = (AddRequest) map.get(mapKey);
        AuthenticatedData entryFromMap = addRequestFromMap.getAuthenticatedData();

        assertEquals(initialSeqNum + 1, entryFromMap.getSequenceNumber());

        assertTrue(entryFromMap instanceof Mailbox);
        Mailbox mailboxFromMap = (Mailbox) entryFromMap;
        NetworkData sealedDataFromMap = mailboxFromMap.getAuthenticatedPayload();
        assertEquals(sealedDataFromMap, mailboxPayload);

        // request inventory with old seqNum
        String dataType = mailboxPayload.getMetaData().getFileName();
        Set<FilterItem> filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        storage.getInventory(filter).ifPresent(inventory -> {
            assertEquals(initialMapSize + 1, inventory.getEntries().size());
        });


        // refresh
        RefreshRequest refreshRequest = storage.getRefreshProtectedDataRequest(mailboxPayload, senderKeyPair);
        Result refreshResult = storage.refreshProtectedStorageEntry(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddRequest) map.get(mapKey);
        entryFromMap = addRequestFromMap.getAuthenticatedData();
        assertEquals(initialSeqNum + 2, entryFromMap.getSequenceNumber());

        // remove
        RemoveMailboxRequest removeMailboxRequest = storage.getRemoveMailboxDataRequest(mailboxPayload, receiverKeyPair);

        Result removeDataResult = storage.removeProtectedStorageEntry(removeMailboxRequest);
        log.error(removeDataResult.toString());
        assertTrue(removeDataResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(Integer.MAX_VALUE, removeRequestFromMap.getSequenceNumber());

        // we must not create a new sealed data as it would have a diff. secret key and so a diff hash...
        // If users re-publish mailbox messages they need to keep the original sealed data and re-use that instead
        // of creating new ones, as otherwise it would appear like a new mailbox msg.
        assertFalse(storage.canAddMailboxMessage(mailboxPayload));
        try {
            // calling getAddMailboxDataRequest without the previous canAddMailboxMessage check will throw
            storage.getAddMailboxDataRequest(mailboxPayload, senderKeyPair, receiverKeyPair.getPublic());
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

        // using the old request again would fail as seq number is not allowing it
        Result result2 = storage.addProtectedStorageEntry(request);
        assertFalse(result2.isSuccess());
        assertTrue(result2.isSequenceNrInvalid());

        // refresh on removed fails
        RefreshRequest refreshRequest2 = storage.getRefreshProtectedDataRequest(mailboxPayload, senderKeyPair);
        Result refreshResult2 = storage.refreshProtectedStorageEntry(refreshRequest);
        assertFalse(refreshResult2.isSuccess());
    }
}
