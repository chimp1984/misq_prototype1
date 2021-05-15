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

package misq.p2p.confidential;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.p2p.Address;
import misq.p2p.Message;

@EqualsAndHashCode(callSuper = true)
@Getter
public class RelayMessage extends ConfidentialMessage {
    private final Address targetAddress;

    public RelayMessage(Message message, Address targetAddress) {
        super(message);
        this.targetAddress = targetAddress;
    }
}
