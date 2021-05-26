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
import misq.common.util.Tuple2;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static misq.common.util.ByteArrayUtils.concat;

/**
 * Using Elliptic Curve Integrated Encryption Scheme for hybrid encryption.
 * <p>
 * Follows roughly the schemes described here:
 * https://cryptobook.nakov.com/asymmetric-key-ciphers/ecies-public-key-encryption
 * https://www.nominet.uk/how-elliptic-curve-cryptography-encryption-works/
 */
@Slf4j
public class ECIESEncryption {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static ConfidentialData encryptAndSign(byte[] message, PublicKey receiverPublicKey, KeyPair senderKeyPair)
            throws GeneralSecurityException {
        // Create shared secret with our private key and receivers public key
        byte[] sharedSecret = SymEncryption.generateSharedSecret(senderKeyPair.getPrivate(), receiverPublicKey);

        // Use that shared secret to derive the hmacKey and the sessionKey
        Tuple2<byte[], byte[]> tuple = deriveKeyMaterial(sharedSecret);
        SecretKey hmacKey = SymEncryption.generateAESKey(tuple.first);
        SecretKey sessionKey = SymEncryption.generateAESKey(tuple.second);

        IvParameterSpec ivSpec = SymEncryption.generateIv();
        byte[] cypherText = SymEncryption.encrypt(message, sessionKey, ivSpec);

        byte[] iv = ivSpec.getIV();
        byte[] senderPublicKey = senderKeyPair.getPublic().getEncoded();
        byte[] receiverPublicKeyBytes = receiverPublicKey.getEncoded();
        byte[] hmac = HmacUtil.createHmac(getHmacInput(iv, cypherText, senderPublicKey, receiverPublicKeyBytes), hmacKey);

        byte[] messageToSign = concat(hmac, cypherText);
        byte[] signature = SignatureUtil.sign(messageToSign, senderKeyPair.getPrivate());
        return new ConfidentialData(null, null, null, hmac, iv, cypherText, signature);
    }

    private static byte[] getHmacInput(byte[] iv, byte[] encryptedMessage, byte[] senderPublicKey, byte[] receiverPublicKey) {
        return concat(iv, encryptedMessage, senderPublicKey, receiverPublicKey);
    }

    public static byte[] decryptAndVerify(ConfidentialData confidentialData, KeyPair receiversKeyPair) throws GeneralSecurityException {
        byte[] senderPublicKeyBytes = confidentialData.getEncryptedSessionKey();
        byte[] hmac = confidentialData.getHmac();
        byte[] iv = confidentialData.getIv();
        byte[] cypherText = confidentialData.getCypherText();
        byte[] signature = confidentialData.getSignature();

        PublicKey senderPublicKey = KeyGeneration.generatePublic(senderPublicKeyBytes);
        byte[] messageToVerify = concat(hmac, cypherText);
        checkArgument(SignatureUtil.verify(messageToVerify, signature, senderPublicKey), "Invalid signature");

        // Create shared secret with our private key and senders public key
        byte[] sharedSecret = SymEncryption.generateSharedSecret(receiversKeyPair.getPrivate(), senderPublicKey);

        Tuple2<byte[], byte[]> tuple = deriveKeyMaterial(sharedSecret);
        SecretKey hmacKey = SymEncryption.generateAESKey(tuple.first);
        SecretKey sessionKey = SymEncryption.generateAESKey(tuple.second);


        byte[] receiverPublicKeyBytes = receiversKeyPair.getPublic().getEncoded();
        byte[] input = getHmacInput(iv, cypherText, senderPublicKeyBytes, receiverPublicKeyBytes);
        checkArgument(HmacUtil.verifyHmac(input, hmacKey, hmac), "Invalid Hmac");

        return SymEncryption.decrypt(cypherText, sessionKey, new IvParameterSpec(iv));
    }

    private static Tuple2<byte[], byte[]> deriveKeyMaterial(byte[] input) {
        // todo causes exceptions as encryption... not clear why
      /*  KDF2BytesGenerator kdf = new KDF2BytesGenerator(new SHA512Digest());
        kdf.init(new KDFParameters(keyInput, iv));
        byte[] out = new byte[512];
        kdf.generateBytes(out, 0, out.length);*/

        byte[] hash = DigestUtil.sha512(input);
        int length = hash.length;
        int from = 0;
        int to = length / 2;
        byte[] macKeyBytes = Arrays.copyOfRange(hash, from, to);
        from = to;
        to = length;
        byte[] sessionKeyBytes = Arrays.copyOfRange(hash, from, to);

        return new Tuple2<>(macKeyBytes, sessionKeyBytes);
    }
}
