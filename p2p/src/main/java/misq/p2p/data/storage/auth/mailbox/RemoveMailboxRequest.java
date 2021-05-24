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

package misq.p2p.data.storage.auth.mailbox;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.security.DigestUtil;
import misq.p2p.data.storage.MetaData;
import misq.p2p.data.storage.auth.AuthenticatedData;
import misq.p2p.data.storage.auth.RemoveRequest;

import java.security.PublicKey;
import java.util.Arrays;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public class RemoveMailboxRequest extends RemoveRequest {
    // Receiver is owner for remove request
    public RemoveMailboxRequest(MetaData metaData,
                                byte[] hash,
                                PublicKey receiverPublicKey,
                                int sequenceNumber,
                                byte[] signature) {
        super(metaData,
                hash,
                receiverPublicKey.getEncoded(),
                receiverPublicKey,
                sequenceNumber,
                signature);
        log.error(this.toString());
    }

    @Override
    public boolean isPublicKeyInvalid(AuthenticatedData entryFromMap) {
        try {
            Mailbox mailbox = (Mailbox) entryFromMap;
            return !Arrays.equals(mailbox.getHashOfReceiversPublicKey(), DigestUtil.sha256(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    public String toString() {
        return "RemoveMailboxDataRequest{} " + super.toString();
    }
}
