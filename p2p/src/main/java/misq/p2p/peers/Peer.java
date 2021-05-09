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

package misq.p2p.peers;

import lombok.Getter;
import misq.p2p.capability.Capability;
import misq.p2p.node.Address;

import java.util.Date;

@Getter
public class Peer {
    private final Capability capability;

    private final long created;

    public Peer(Capability capability) {
        this.capability = capability;
        this.created = System.currentTimeMillis();
    }

    public Date getDate() {
        return new Date(created);
    }

    public Address getAddress() {
        return capability.getAddress();
    }
}