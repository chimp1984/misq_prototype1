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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.p2p.Address;
import misq.p2p.node.capability.Capability;
import misq.p2p.node.connection.RawConnection;

@Slf4j
public class Connection {
    private final RawConnection rawConnection;
    @Getter
    private final Capability capability;
    @Getter
    private final String id;

    public Connection(RawConnection rawConnection, Capability capability) {
        this.rawConnection = rawConnection;
        this.capability = capability;

        id = rawConnection.getId();
    }

    public Address getPeerAddress() {
        return capability.getAddress();
    }

    RawConnection getRawConnection() {
        return rawConnection;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "\n     id='" + id + '\'' +
                ",\n     peerAddress=" + getPeerAddress() +
                ",\n     capability=" + capability +
                "\n}";
    }
}
