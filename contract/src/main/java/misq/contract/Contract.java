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

package misq.contract;


import lombok.Getter;

import java.util.Map;

@Getter
public class Contract {
    private final Map<Role, Protocol> protocols;
    private final Role myRole;
    private final Peer peer;

    public Contract(Map<Role, Protocol> protocols, Role myRole, Peer peer) {
        this.protocols = protocols;
        this.myRole = myRole;
        this.peer = peer;
    }

    public Protocol getProtocol(Role role) {
        return protocols.get(role);
    }
}
