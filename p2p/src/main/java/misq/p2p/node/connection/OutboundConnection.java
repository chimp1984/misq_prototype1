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

package misq.p2p.node.connection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.StringUtils;
import misq.p2p.Address;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class OutboundConnection extends RawConnection {
    @Getter
    private final Address address;

    public OutboundConnection(Socket socket, Address address) throws IOException {
        super(socket);

        this.address = address;
        log.debug("Create outboundConnection to {}", address);
    }

    @Override
    public String toString() {
        return StringUtils.truncate(address.toString()) + " / " + id;
    }
}
