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

package misq.common.security.legacy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyPairGeneratorUtil {
    public static final String RSA = "RSA";
    public static final int KEY_SIZE = 2048;

    private static KeyFactory getKeyFactory() throws NoSuchAlgorithmException {
        return KeyFactory.getInstance(RSA);
    }

    public static KeyPair generateKeyPair() throws GeneralSecurityException {
        return generateKeyPair(KEY_SIZE);
    }

    public static KeyPair generateKeyPair(int keySize) throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(keySize);
        KeyPair pair = generator.generateKeyPair();
        PrivateKey privateKey = pair.getPrivate();
        PublicKey publicKey = pair.getPublic();
        return new KeyPair(publicKey, privateKey);
    }

    public static PublicKey generatePublic(PrivateKey privateKey) throws GeneralSecurityException {
        KeyFactory keyFactory = getKeyFactory();
        RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static PublicKey generatePublic(byte[] encodedKey) throws GeneralSecurityException {
        KeyFactory keyFactory = getKeyFactory();
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);
        return keyFactory.generatePublic(publicKeySpec);
    }

    public static PrivateKey generatePrivate(byte[] encodedKey) throws GeneralSecurityException {
        KeyFactory keyFactory = getKeyFactory();
        EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(encodedKey);
        return keyFactory.generatePrivate(publicKeySpec);
    }

    public static void write(byte[] encodedKey, String fileName) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(encodedKey);
        }
    }

    public static byte[] readKey(String fileName) throws IOException {
        return Files.readAllBytes(new File(fileName).toPath());
    }
}
