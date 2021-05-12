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

package misq.p2p.proxy;

import misq.p2p.NetworkConfig;
import misq.p2p.endpoint.Address;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface NetworkProxy {
    static NetworkProxy get(NetworkConfig networkConfig) {
        switch (networkConfig.getNetworkType()) {
            case TOR:
                return new TorNetworkProxy(networkConfig);
           /* case I2P:
                return  new TorNetworkNode(config);*/
            case CLEAR:
            default:
                return new ClearNetNetworkProxy(networkConfig);
        }
    }

    enum State {
        NOT_STARTED,
        INITIALIZED,
        SERVER_SOCKET_CREATED,
        SOCKET_CREATED,
        HANDSHAKE_DONE,
        SHUTTING_DOWN,
        SHUT_DOWN
    }

    /**
     * Initializes the NetworkProxy
     *
     * @return True if initialisation was successful
     */
    CompletableFuture<Boolean> initialize();

    /**
     * Creates the server socket (e.g. hidden service)
     *
     * @param serverId
     * @param serverPort
     * @return ServerInfo encapsulating ServerSocket and the server address (e.g. onion address)
     */
    CompletableFuture<GetServerSocketResult> getServerSocket(String serverId, int serverPort);

    /**
     * Returns a client socket
     *
     * @param address
     * @return
     * @throws IOException
     */
    Socket getSocket(Address address) throws IOException;


    State getState();

    Optional<Address> getServerAddress(String serverId);

    void shutdown();
}
