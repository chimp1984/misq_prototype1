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

package misq.p2p.node;

import lombok.extern.slf4j.Slf4j;
import misq.p2p.message.Message;
import misq.p2p.node.protection.GuardedMessage;
import misq.p2p.node.protection.PermissionControl;

import java.util.Optional;

@Slf4j
public class MessageFilter {
    private final PermissionControl permissionControl;

    public MessageFilter(PermissionControl permissionControl) {
        this.permissionControl = permissionControl;
    }

    public Optional<Message> process(Message message) {
        if (message instanceof GuardedMessage) {
            GuardedMessage guardedMessage = (GuardedMessage) message;
            if (permissionControl.hasPermit(guardedMessage)) {
                return Optional.of(guardedMessage.getPayload());
            }
        }
        return Optional.empty();
    }
}
