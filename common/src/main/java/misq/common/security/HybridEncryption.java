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
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

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
public class HybridEncryption {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static ConfidentialData encryptAndSign(byte[] message, PublicKey receiverPublicKey, KeyPair senderKeyPair)
            throws GeneralSecurityException {
        SecretKey sessionKey = SymEncryption.generateAESKey();
        SecretKey hmacKey = SymEncryption.generateAESKey();
        IvParameterSpec ivSpec = SymEncryption.generateIv();

        byte[] encryptedSessionKey = AsymEncryption.encrypt(sessionKey.getEncoded(), (ECPublicKey) receiverPublicKey);
        byte[] cypherText = SymEncryption.encrypt(message, sessionKey, ivSpec);
        byte[] encryptedHmacKey = SymEncryption.encrypt(hmacKey.getEncoded(), sessionKey, ivSpec);
        byte[] encryptedSenderPubKey = SymEncryption.encrypt(senderKeyPair.getPublic().getEncoded(), sessionKey, ivSpec);

        byte[] iv = ivSpec.getIV();
        byte[] hmacInput = getHmacInput(cypherText, iv, encryptedSessionKey, encryptedSenderPubKey);
        byte[] hmac = HmacUtil.createHmac(hmacInput, hmacKey);

        byte[] sigInput = getSigInput(encryptedSessionKey, encryptedHmacKey, encryptedSenderPubKey, hmac, iv, cypherText);
        byte[] signature = SignatureUtil.sign(sigInput, senderKeyPair.getPrivate());

        return new ConfidentialData(encryptedSessionKey, encryptedHmacKey, encryptedSenderPubKey, hmac, iv, cypherText, signature);
    }

    public static byte[] decryptAndVerify(ConfidentialData confidentialData, KeyPair receiversKeyPair) throws GeneralSecurityException {
        byte[] encryptedSessionKey = confidentialData.getEncryptedSessionKey();
        byte[] encryptedHmacKey = confidentialData.getEncryptedHmacKey();
        byte[] encryptedSenderPubKey = confidentialData.getEncryptedSenderPubKey();
        byte[] hmac = confidentialData.getHmac();
        byte[] iv = confidentialData.getIv();
        byte[] cypherText = confidentialData.getCypherText();
        byte[] signature = confidentialData.getSignature();

        byte[] encodedSessionKey = AsymEncryption.decrypt(encryptedSessionKey, (ECPrivateKey) receiversKeyPair.getPrivate());
        SecretKey sessionKey = SymEncryption.generateAESKey(encodedSessionKey);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        byte[] encodedHmacKey = SymEncryption.decrypt(encryptedHmacKey, sessionKey, ivSpec);
        SecretKey hmacKey = SymEncryption.generateAESKey(encodedHmacKey);

        byte[] hmacInput = getHmacInput(cypherText, iv, encryptedSessionKey, encryptedSenderPubKey);
        checkArgument(HmacUtil.verifyHmac(hmacInput, hmacKey, hmac), "Invalid Hmac");

        byte[] encodedSenderPubKey = SymEncryption.decrypt(encryptedSenderPubKey, sessionKey, ivSpec);
        PublicKey senderPubKey = KeyGeneration.generatePublic(encodedSenderPubKey);

        byte[] sigInput = getSigInput(encryptedSessionKey, encryptedHmacKey, encryptedSenderPubKey, hmac, iv, cypherText);
        checkArgument(SignatureUtil.verify(sigInput, signature, senderPubKey), "Invalid signature");

        return SymEncryption.decrypt(cypherText, sessionKey, ivSpec);
    }

    private static byte[] getHmacInput(byte[] cypherText, byte[] iv, byte[] encryptedSessionKey, byte[] encryptedSenderPubKey) {
        return concat(encryptedSessionKey, encryptedSenderPubKey, iv, cypherText);
    }

    private static byte[] getSigInput(byte[] encryptedSessionKey, byte[] encryptedHmacKey, byte[] encryptedSenderPubKey, byte[] hmac, byte[] iv, byte[] cypherText) {
        return concat(encryptedSessionKey, encryptedHmacKey, encryptedSenderPubKey, hmac, iv, cypherText);
    }
}
