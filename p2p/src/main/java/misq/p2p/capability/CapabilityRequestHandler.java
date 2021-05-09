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

package misq.p2p.capability;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.Disposable;
import misq.p2p.NetworkType;
import misq.p2p.node.Address;
import misq.p2p.node.Connection;
import misq.p2p.node.Message;
import misq.p2p.node.MessageListener;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CapabilityRequestHandler implements MessageListener, Disposable {
    private static final long TIMEOUT_SEC = 90;

    private final Connection connection;
    private final Address peersAddress;
    private final Address myAddress;
    private final Set<NetworkType> mySupportedNetworkTypes;
    private final long requestNonce = new Random().nextLong();
    private final CompletableFuture<Capability> future = new CompletableFuture<>();

    public CapabilityRequestHandler(Connection connection,
                                    Address peersAddress,
                                    Address myAddress,
                                    Set<NetworkType> mySupportedNetworkTypes) {
        this.connection = connection;
        this.peersAddress = peersAddress;
        this.myAddress = myAddress;
        this.mySupportedNetworkTypes = mySupportedNetworkTypes;
    }

    public CompletableFuture<Capability> request() {
        future.orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
        connection.addMessageListener(this);
        Capability capability = new Capability(myAddress, mySupportedNetworkTypes);
        connection.send(new CapabilityRequest(capability, requestNonce));
        return future;
    }

    public void dispose() {
        connection.removeMessageListener(this);
        future.cancel(true);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof CapabilityResponse) {
            CapabilityResponse capabilityResponse = (CapabilityResponse) message;
            Capability capability = capabilityResponse.getCapability();
            if (capabilityResponse.getRequestNonce() != requestNonce) {
                log.warn("Responded nonce {} does not match requestNonce {}",
                        capabilityResponse.getRequestNonce(), requestNonce);
                connection.close();
                future.completeExceptionally(new Exception("Invalid HandshakeResponse"));
            }
            if (!peersAddress.equals(capability.getAddress())) {
                log.warn("Responded address {} does not match peersAddress {}",
                        capability.getAddress(), peersAddress);

                connection.close();
                future.completeExceptionally(new Exception("Invalid HandshakeResponse"));
            }

            connection.removeMessageListener(this);
            future.complete(capability);
        }
    }
}
