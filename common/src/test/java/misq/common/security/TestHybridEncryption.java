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
import misq.common.util.ByteArrayUtils;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SignatureException;

import static org.junit.Assert.*;

@Slf4j
public class TestHybridEncryption {

    @Test
    public void testHybridEncryption() throws GeneralSecurityException {
        byte[] message = "hello".getBytes();
        KeyPair keyPairSender = KeyGeneration.generateKeyPair();
        KeyPair keyPairReceiver = KeyGeneration.generateKeyPair();

        ConfidentialData confidentialData = HybridEncryption.encrypt(message, keyPairReceiver.getPublic(), keyPairSender);
        byte[] decrypted = HybridEncryption.decrypt(confidentialData, keyPairReceiver, keyPairSender.getPublic());
        assertArrayEquals(message, decrypted);

        // failure cases
        byte[] hmac = confidentialData.getHmac();
        byte[] iv = confidentialData.getIv();
        byte[] encryptedMessage = confidentialData.getCypherText();
        byte[] signature = confidentialData.getSignature();

        KeyPair fakeKeyPair = KeyGeneration.generateKeyPair();
        byte[] bitStream = ByteArrayUtils.concat(hmac, encryptedMessage);
        byte[] fakeSignature = SignatureUtil.sign(bitStream, fakeKeyPair.getPrivate());
        ConfidentialData withFakeSigAndPubKey = new ConfidentialData(hmac, iv, encryptedMessage, fakeSignature);
        try {
            // Expect to fail as pub key in method call not matching the one in sealed data
            HybridEncryption.decrypt(withFakeSigAndPubKey, keyPairReceiver, keyPairSender.getPublic());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        // fake sig or fake signed message throw SignatureException
        try {
            ConfidentialData withFakeSig = new ConfidentialData(hmac, iv, encryptedMessage, "signature".getBytes());
            HybridEncryption.decrypt(withFakeSig, keyPairReceiver, keyPairSender.getPublic());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof SignatureException);
        }

        // fake iv
        try {
            ConfidentialData withFakeSig = new ConfidentialData(hmac, "iv".getBytes(), encryptedMessage, signature);
            HybridEncryption.decrypt(withFakeSig, keyPairReceiver, keyPairSender.getPublic());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }

        // fake hmac
        try {
            ConfidentialData withFakeSig = new ConfidentialData("hmac".getBytes(), iv, encryptedMessage, signature);
            HybridEncryption.decrypt(withFakeSig, keyPairReceiver, keyPairSender.getPublic());
            fail();
        } catch (Throwable e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
