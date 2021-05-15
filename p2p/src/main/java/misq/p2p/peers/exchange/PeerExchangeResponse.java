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

package misq.p2p.peers.exchange;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.p2p.Message;
import misq.p2p.peers.Peer;

import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode
@Getter
class PeerExchangeResponse implements Message {
    private final Set<Peer> peers;
    private final String uid;

    public PeerExchangeResponse(Set<Peer> peers) {
        this.peers = peers;
        uid = UUID.randomUUID().toString();
    }

    @Override
    public String toString() {
        return "PeerExchangeResponse{" +
                "\n     peers=" + peers +
                ",\n     uid='" + uid + '\'' +
                "\n}";
    }
}
