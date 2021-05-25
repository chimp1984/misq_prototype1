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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Arrays;

@Slf4j
public class Hmac {
    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static final String HMAC = "HmacSHA256";

    public static boolean verifyHmac(byte[] message, byte[] hmac, SecretKey secretKey) throws GeneralSecurityException {
        byte[] hmacTest = createHmac(message, secretKey);
        return Arrays.equals(hmacTest, hmac);
    }

    public static byte[] createHmac(byte[] data, SecretKey secretKey) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC);
        mac.init(secretKey);
        return mac.doFinal(data);
    }
}
