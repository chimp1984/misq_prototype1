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

import lombok.extern.slf4j.Slf4j;
import misq.common.security.DigestUtil;
import misq.common.security.KeyPairGeneratorUtil;
import misq.common.security.SignatureUtil;
import misq.common.util.Hex;
import misq.common.util.OsUtils;
import misq.p2p.data.storage.MapKey;
import misq.p2p.data.storage.MockAuthorizedData;
import misq.p2p.data.storage.MockProtectedData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class AuthorizedDataStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testAddAndRemove() throws GeneralSecurityException, IOException {
        String privateKeyAsHex = "308204bd020100300d06092a864886f70d0101010500048204a7308204a30201000282010100b2020f519d4538bc7820b6f6134be47ea1b4cdfc11b8e3191caf2c62f9718c0bf538975c86a60a7a3444baa24fd23e85056d81c9f1aae429d316c02d21ac596d895a68574c2b0b7da7c437c598548f6d9d57e42917073c60fc0e2c8ab646df634a7fbde2561ba3f0bf773dca1adcde12fc210d57737d641b9e7a1d3a190857b2b3d1a7e7feadced0d36c2dd48f02096eb445bcd6b27bfa87bfc6310a6ff3a5bf546abda0946b56433c99feda10c68716a22e2c8c6201357dc5dc122c6edd118364d9fc62539c30dfd77cd08990a382da27ce672f228903ffce10000c92e8ef9d114d3cd578f7bccac644ec2f01477c839c3fe2dcd48ff0d79754ccf18d93efd902030100010282010052322d4cac6868586ec7fc74085904fb8c43e6d1bc853928415c8a0c71ce82b28adb44d94ebba11ee8bfcbc663415be924a67a45fbb7c37fa49f5ec1f848934ac71fde0a617b86e4d0f10427d3bd48357f703444dd0354581c4b9739be0d5fdd836b150d4c619b03f205679cc966af8ce8ab82739d3a8b5d4d8269b88812981b7e83af269a0c237234634b57c278fcad0f1735ab1bad3311d908e8bfaa29dcff319bfac9500678f3be1292bc0f32e96ccd52277e263b7aaa778d4f7bdb711650f99d53c32b5fbcef477093f676475189878798146c10f96eecc1ebe5b80449b207ce78a704663ed8a4a2437bcdb4b52909c6b240b9bd2067aff5bb33beef12f902818100fcb4ad42ea5d5076febc41a81211415a8d1a8f6f645343df0cec5ade7e71c1ed94b397be2637f81bb1973c724952c6c445c940218bbf53ed1e1c0ca20aa03cfd0116093fd02e80ffcef77ddbf4c63466ac4155cc474eec5d9c709a5b3a0972dfdc32aedb0ed65c735f19f02037a8034568f83261f5f6c67e6245797cb8585fd702818100b4541a8d2e020b7c4bc89c09735759e703ec9f29e0d7e709c74855f8e42de5074f1c3386f16cef3fcbca654b97bfe18d897546f6fa8488b518c530a831e103681f536d67501f668e90c9718bded929b231262ba09346a22a9c00679b8e6bd95565b0ca6661d455817c16c3f894759be0cccdf0cf7f7b406761d960f1f6caf7cf028181009cdce6841808ce8edef72ae65238c5d198af39041349a062cf99d39a32f118490aba2462534500cce80311f17b5457afb40605ba0d0e39e1818435cf4c3b454063b133129a7e9372b71d67d1e672364ad97840f2e9fcb2ba3506acba1e1f89602e4683c5d4c2f966604d30823f2a1ac5b63002ce4e28ddf3cbba867c05ce4dc702818021e9b3389bcf6ca38a8906b74c46c0348eeb601f7b167f6fba57a33b7486210d57d660e65edef2bb97b2cf8c00d4e8313b09a037f0731e56987af5249c84c9a43a47f14a3daa3a1a53a65ec1443ea8f5c7027bafee22997ad3edcb8e58a175b4f6b3e1cc915762614099f36efb5486e526ff0feba5f8e2eace5f1839490570c70281807c74da2b71789f5abbb087c3540514def5f6db7314a9b73b483a0b1db17ebe0779ad25fc1281c8d128a81c3d9a598615be4f11055afb0679d8f3ebd7a6573e644fd56a390ac61886c5f6ed960a1c7ad94e17e1c28650bf56bcf250ba074470daa29276bdb66b874b3bc14a3cc1fc28ec5457640142a9f4326a0034c5b913debd";
        byte[] privateKeyBytes = Hex.decode(privateKeyAsHex);
        PrivateKey privateKey = KeyPairGeneratorUtil.generatePrivate(privateKeyBytes);
        PublicKey publicKey = KeyPairGeneratorUtil.generatePublic(privateKey);
        AuthenticatedPayload payload = new MockProtectedData("test" + UUID.randomUUID().toString());
        byte[] signature = SignatureUtil.sign(payload.serialize(), privateKey);
        MockAuthorizedData data = new MockAuthorizedData(payload, signature, publicKey);

        KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();

        AuthenticatedDataStore store = new AuthenticatedDataStore(appDirPath, data.getMetaData());
        AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, data, keyPair);
        byte[] hash = DigestUtil.sha256(data.serialize());
        int initialSeqNum = store.getSequenceNumber(hash);
        Result result = store.add(addRequest);
        assertTrue(result.isSuccess());

        ConcurrentHashMap<MapKey, AuthenticatedDataRequest> map = store.getMap();
        MapKey mapKey = new MapKey(hash);
        AddAuthenticatedDataRequest addRequestFromMap = (AddAuthenticatedDataRequest) map.get(mapKey);
        AuthenticatedData dataFromMap = addRequestFromMap.getAuthenticatedData();

        assertEquals(initialSeqNum + 1, dataFromMap.getSequenceNumber());
        assertEquals(dataFromMap.getPayload(), payload);

        // refresh
        RefreshRequest refreshRequest = RefreshRequest.from(store, data, keyPair);
        Result refreshResult = store.refresh(refreshRequest);
        assertTrue(refreshResult.isSuccess());

        addRequestFromMap = (AddAuthenticatedDataRequest) map.get(mapKey);
        dataFromMap = addRequestFromMap.getAuthenticatedData();
        assertEquals(initialSeqNum + 2, dataFromMap.getSequenceNumber());

        //remove
        RemoveRequest removeRequest = RemoveRequest.from(store, data, keyPair);
        Result removeDataResult = store.remove(removeRequest);
        assertTrue(removeDataResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(initialSeqNum + 3, removeRequestFromMap.getSequenceNumber());
    }
}
