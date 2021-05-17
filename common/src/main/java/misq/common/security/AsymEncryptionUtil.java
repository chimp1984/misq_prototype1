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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;

@Slf4j
public class AsymEncryptionUtil {
    public static final String RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1PADDING";

    public static byte[] encryptSecretKey(SecretKey secretKey, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.WRAP_MODE, publicKey, getOAEPParameterSpec());
        return cipher.wrap(secretKey);
    }

    public static SecretKey decryptSecretKey(byte[] encryptedSecretKey, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.UNWRAP_MODE, privateKey, getOAEPParameterSpec());
        return (SecretKey) cipher.unwrap(encryptedSecretKey, "AES", Cipher.SECRET_KEY);
    }

    private static OAEPParameterSpec getOAEPParameterSpec() {
        return new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    }
}
