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

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class SignatureUtil {
    public static final String SHA_256_WITH_RSA = "SHA256withRSA";

    public static byte[] sign(byte[] message, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SHA_256_WITH_RSA);
        signature.initSign(privateKey);
        signature.update(message);
        return signature.sign();
    }

    public static boolean verify(byte[] message, byte[] signature, PublicKey publicKey) throws GeneralSecurityException {
        Signature sig = Signature.getInstance(SHA_256_WITH_RSA);
        sig.initVerify(publicKey);
        sig.update(message);
        return sig.verify(signature);
    }
}
