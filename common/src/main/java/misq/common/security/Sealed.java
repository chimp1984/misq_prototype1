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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.common.util.Hex;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
public class Sealed implements Serializable {
    private final byte[] encryptedHmacSessionKey; // 256 bytes
    private final byte[] encryptedSessionKey;// 256 bytes
    private final byte[] hmac;// 32 bytes
    private final byte[] iv;// 16 bytes
    private final byte[] encryptedMessage;
    private final byte[] signature;// 256 bytes
    private final byte[] senderPublicKey;// 294 bytes

    public Sealed(byte[] encryptedHmacSessionKey,
                  byte[] encryptedSessionKey,
                  byte[] hmac,
                  byte[] iv,
                  byte[] encryptedMessage,
                  byte[] signature,
                  byte[] senderPublicKey) {
        this.encryptedHmacSessionKey = encryptedHmacSessionKey;
        this.encryptedSessionKey = encryptedSessionKey;
        this.hmac = hmac;
        this.iv = iv;
        this.encryptedMessage = encryptedMessage;
        this.signature = signature;
        this.senderPublicKey = senderPublicKey;
    }

    @Override
    public String toString() {
        return "Sealed{" +
                "\n     encryptedHmacSessionKey=" + Hex.encode(encryptedHmacSessionKey) +
                ",\n     encryptedSessionKey=" + Hex.encode(encryptedSessionKey) +
                ",\n     hmac=" + Hex.encode(hmac) +
                ",\n     iv=" + Hex.encode(iv) +
                ",\n     encryptedMessage=" + Hex.encode(encryptedMessage) +
                ",\n     signature=" + Hex.encode(signature) +
                ",\n     senderPublicKey=" + Hex.encode(senderPublicKey) +
                "\n}";
    }
}
