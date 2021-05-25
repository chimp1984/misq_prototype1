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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class SignatureTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testSignature() {
        byte[] message = "hello".getBytes();
        try {
            KeyPair keyPair = KeyPairGeneratorUtil.generateKeyPair();
            byte[] signature = SignatureUtil.sign(message, keyPair.getPrivate());
            boolean result = SignatureUtil.verify(message, signature, keyPair.getPublic());
            assertTrue(result);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            fail();
        }
    }
}
