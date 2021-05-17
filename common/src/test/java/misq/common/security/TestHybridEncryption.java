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

package misq.common.security;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SignatureException;

import static org.junit.Assert.assertArrayEquals;

@Slf4j
public class TestHybridEncryption {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testHybridEncryption() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        long ts = System.currentTimeMillis();
        KeyPair keyPairSender = KeyPairGeneratorUtil.generateKeyPair();
        KeyPair keyPairReceiver = KeyPairGeneratorUtil.generateKeyPair();
        log.error("Generating 2 key pairs took {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        Seal seal = HybridEncryption.encrypt(message, keyPairReceiver.getPublic(), keyPairSender);
        log.error("Encryption took {} ms", System.currentTimeMillis() - ts);

        ts = System.currentTimeMillis();
        byte[] decrypted = HybridEncryption.decrypt(seal, keyPairReceiver.getPrivate());
        assertArrayEquals(message, decrypted);
        log.error("Decryption took {} ms", System.currentTimeMillis() - ts);

        byte[] encryptedHmacSessionKey = seal.getEncryptedHmacSessionKey();
        byte[] encryptedSessionKey = seal.getEncryptedSessionKey();
        byte[] hmac = seal.getHmac();
        byte[] iv = seal.getIv();
        byte[] encryptedMessage = seal.getEncryptedMessage();
        byte[] signature = seal.getSignature();
        byte[] senderPublicKey = seal.getSenderPublicKey();

        // If sig and pubkey are matching but changed we do not detect that. Receiver need to check
        // if pubkey is the expected one. This is outside of the scope of the HybridEncryption.
        KeyPair fakeKeyPair = KeyPairGeneratorUtil.generateKeyPair();
        byte[] bitStream = HybridEncryption.getBitStream(encryptedHmacSessionKey, encryptedSessionKey, hmac, iv, encryptedMessage);
        byte[] fakeSignature = SignatureUtil.sign(bitStream, fakeKeyPair.getPrivate());
        Seal withFakeSigAndPubKey = new Seal(encryptedHmacSessionKey, encryptedSessionKey, hmac, iv,
                encryptedMessage, fakeSignature, fakeKeyPair.getPublic().getEncoded());
        decrypted = HybridEncryption.decrypt(withFakeSigAndPubKey, keyPairReceiver.getPrivate());
        assertArrayEquals(message, decrypted);


        // fake sig or fake signed message throw SignatureException
        try {
            Seal withFakeSig = new Seal(encryptedHmacSessionKey, encryptedSessionKey, hmac, iv,
                    encryptedMessage, "signature".getBytes(), senderPublicKey);
            HybridEncryption.decrypt(withFakeSig, keyPairReceiver.getPrivate());
            expectedException.expect(SignatureException.class);
        } catch (Throwable ignore) {
        }
        try {
            Seal withFakeIv = new Seal(encryptedHmacSessionKey, encryptedSessionKey, hmac, "iv".getBytes(),
                    encryptedMessage, signature, senderPublicKey);
            HybridEncryption.decrypt(withFakeIv, keyPairReceiver.getPrivate());
            expectedException.expect(SignatureException.class);
        } catch (Throwable ignore) {
        }
    }
}
