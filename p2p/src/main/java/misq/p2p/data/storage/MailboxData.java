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

package misq.p2p.data.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.Hex;

@Getter
@EqualsAndHashCode(callSuper = true)
@Slf4j
public final class MailboxData extends ProtectedData {
    private final byte[] hashOfReceiversPublicKey;

    public MailboxData(SealedData sealedData,
                       byte[] hashOfSendersPublicKey,
                       byte[] hashOfReceiversPublicKey) {
        super(sealedData, hashOfSendersPublicKey);

        this.hashOfReceiversPublicKey = hashOfReceiversPublicKey;
    }

    @Override
    public String toString() {
        return "MailboxData{" +
                "\n     hashOfReceiversPublicKey=" + Hex.encode(hashOfReceiversPublicKey) +
                "\n} " + super.toString();
    }
}
