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
import misq.common.security.DigestUtil;

import java.security.PublicKey;
import java.util.Arrays;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public class RemoveMailboxDataRequest extends RemoveProtectedDataRequest {
    // Receiver is owner for remove request
    public RemoveMailboxDataRequest(String storageFileName,
                                    byte[] dataHash,
                                    PublicKey receiverPublicKey,
                                    int sequenceNumber,
                                    byte[] signature) {
        super(storageFileName,
                dataHash,
                receiverPublicKey.getEncoded(),
                receiverPublicKey,
                sequenceNumber,
                signature);
    }

    @Override
    public boolean isPublicKeyInvalid(ProtectedData protectedData) {
        try {
            MailboxData mailboxData = (MailboxData) protectedData;
            return !Arrays.equals(mailboxData.getHashOfReceiversPublicKey(), DigestUtil.sha256(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }
}
