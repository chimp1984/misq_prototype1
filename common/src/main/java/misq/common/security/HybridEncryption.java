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

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static com.google.common.base.Preconditions.checkArgument;

public class HybridEncryption {
    public static Seal encrypt(byte[] message, PublicKey receiverPublicKey, KeyPair senderKeyPair) throws GeneralSecurityException {
        // Symmetric key setup
        SecretKey sessionKey = SymEncryptionUtil.generateAESKey();

        // Encrypt sessionKey
        byte[] encryptedSessionKey = AsymEncryptionUtil.encryptSecretKey(sessionKey, receiverPublicKey);

        // Encrypt message
        IvParameterSpec ivSpec = SymEncryptionUtil.generateIv();
        byte[] encryptedMessage = SymEncryptionUtil.encrypt(message, sessionKey, ivSpec);

        // Hmac of encryptedMessage
        SecretKey hmacSessionKey = SymEncryptionUtil.generateAESKey();
        byte[] hmac = Hmac.createHmac(encryptedMessage, hmacSessionKey);
        byte[] encryptedHmacSessionKey = AsymEncryptionUtil.encryptSecretKey(hmacSessionKey, receiverPublicKey);

        // Combine bitStream of all data
        byte[] iv = ivSpec.getIV();
        byte[] bitStream = getBitStream(encryptedHmacSessionKey, encryptedSessionKey, hmac, iv, encryptedMessage);

        // Create signature over bitstream
        byte[] signature = SignatureUtil.sign(bitStream, senderKeyPair.getPrivate());

        return new Seal(encryptedHmacSessionKey, encryptedSessionKey, hmac, iv, encryptedMessage, signature,
                senderKeyPair.getPublic().getEncoded());
    }

    public static byte[] decrypt(Seal seal, PrivateKey privateKey) throws GeneralSecurityException {
        byte[] encryptedHmacSessionKey = seal.getEncryptedHmacSessionKey();
        byte[] encryptedSessionKey = seal.getEncryptedSessionKey();
        byte[] hmac = seal.getHmac();
        byte[] iv = seal.getIv();
        byte[] encryptedMessage = seal.getEncryptedMessage();
        byte[] signature = seal.getSignature();
        PublicKey senderPublicKey = KeyPairGeneratorUtil.generatePublic(seal.getSenderPublicKey());

        // Create bitstream
        byte[] bitStream = getBitStream(encryptedHmacSessionKey, encryptedSessionKey, hmac, iv, encryptedMessage);

        // Verify signature
        checkArgument(SignatureUtil.verify(bitStream, signature, senderPublicKey), "Invalid signature");

        // Decrypt encryptedHmacSessionKey
        SecretKey hmacSessionKey = AsymEncryptionUtil.decryptSecretKey(encryptedHmacSessionKey, privateKey);

        // Verify hmac
        checkArgument(Hmac.verifyHmac(encryptedMessage, hmac, hmacSessionKey), "Invalid Hmac");

        // Decrypt encryptedSessionKey
        SecretKey sessionKey = AsymEncryptionUtil.decryptSecretKey(encryptedSessionKey, privateKey);

        // Decrypt encryptedMessage
        return SymEncryptionUtil.decrypt(encryptedMessage, sessionKey, new IvParameterSpec(iv));
    }

    public static byte[] getBitStream(byte[] encryptedHmacSessionKey,
                                      byte[] encryptedSessionKey,
                                      byte[] hmac,
                                      byte[] iv,
                                      byte[] encryptedMessage) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(encryptedHmacSessionKey.length +
                encryptedSessionKey.length +
                hmac.length +
                iv.length +
                encryptedMessage.length);
        byteBuffer.put(encryptedHmacSessionKey);
        byteBuffer.put(encryptedSessionKey);
        byteBuffer.put(hmac);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedMessage);
        return byteBuffer.array();
    }
}
