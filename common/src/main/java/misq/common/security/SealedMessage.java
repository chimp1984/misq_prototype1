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

import lombok.Getter;

@Getter
public class SealedMessage {
    private final byte[] encryptedHmacSessionKey;
    private final byte[] encryptedSessionKey;
    private final byte[] hmac;
    private final byte[] iv;
    private final byte[] encryptedMessage;
    private final byte[] signature;
    private final byte[] publicKey;

    public SealedMessage(byte[] encryptedHmacSessionKey,
                         byte[] encryptedSessionKey,
                         byte[] hmac,
                         byte[] iv,
                         byte[] encryptedMessage,
                         byte[] signature,
                         byte[] publicKey) {
        this.encryptedHmacSessionKey = encryptedHmacSessionKey;
        this.encryptedSessionKey = encryptedSessionKey;
        this.hmac = hmac;
        this.iv = iv;
        this.encryptedMessage = encryptedMessage;
        this.signature = signature;
        this.publicKey = publicKey;
    }
}
