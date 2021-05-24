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

package misq.p2p.data.storage.mailbox;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.p2p.data.storage.auth.AddRequest;

import java.security.PublicKey;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public class AddMailboxRequest extends AddRequest {
    public AddMailboxRequest(MailboxData mailboxData, byte[] signature, PublicKey senderPublicKey) {
        super(mailboxData, signature, senderPublicKey);
    }

    @Override
    public String toString() {
        return "AddMailboxDataRequest{} " + super.toString();
    }
}
