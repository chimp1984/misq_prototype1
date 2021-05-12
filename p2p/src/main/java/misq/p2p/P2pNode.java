/*
 * This file is part of Misq.
 *
 * Misq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Misq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Misq. If not, see <http://www.gnu.org/licenses/>.
 */

package misq.p2p;


import com.google.common.annotations.VisibleForTesting;
import misq.p2p.capability.CapabilityAwareNode;
import misq.p2p.confidential.ConfidentialMessageService;
import misq.p2p.data.DataService;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.data.storage.Storage;
import misq.p2p.endpoint.*;
import misq.p2p.peers.PeerConfig;
import misq.p2p.peers.PeerGroup;
import misq.p2p.peers.PeerManager;
import misq.p2p.peers.exchange.DefaultPeerExchangeStrategy;
import misq.p2p.protection.ProtectedNode;
import misq.p2p.proxy.GetServerSocketResult;
import misq.p2p.proxy.NetworkProxy;
import misq.p2p.router.gossip.GossipResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * High level API for the p2p network.
 */
public class P2pNode {
    private static final Logger log = LoggerFactory.getLogger(P2pNode.class);

    private final NetworkConfig networkConfig;
    private final Storage storage;
    private final EndPoint endPoint;
    private final PeerManager peerManager;
    private final ProtectedNode protectedNode;
    private final ConfidentialMessageService confidentialMessageService;
    private final DataService dataService;
    private final CapabilityAwareNode capabilityAwareNode;
    private final NetworkProxy networkProxy;

    public P2pNode(NetworkConfig networkConfig, Set<NetworkType> mySupportedNetworks, Storage storage) {
        this.networkConfig = networkConfig;
        this.storage = storage;

        networkProxy = NetworkProxy.get(networkConfig);
        endPoint = new EndPoint(networkProxy);
        capabilityAwareNode = new CapabilityAwareNode(endPoint, mySupportedNetworks);
        protectedNode = new ProtectedNode(capabilityAwareNode);


        PeerConfig peerConfig = networkConfig.getPeerConfig();
        PeerGroup peerGroup = new PeerGroup(protectedNode, peerConfig);
        DefaultPeerExchangeStrategy peerExchangeStrategy = new DefaultPeerExchangeStrategy(peerGroup, peerConfig);
        peerManager = new PeerManager(protectedNode, peerGroup, peerExchangeStrategy, peerConfig);

        confidentialMessageService = new ConfidentialMessageService(protectedNode, peerGroup);

        dataService = new DataService(protectedNode, peerGroup, storage);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<GetServerSocketResult> initializeServer() {
        return protectedNode.initializeServer(networkConfig.getServerId(), networkConfig.getServerPort());
    }

    public CompletableFuture<Boolean> bootstrap() {
        return peerManager.bootstrap(networkConfig.getServerId(), networkConfig.getServerPort());
    }

    public CompletableFuture<Connection> confidentialSend(Message message, Address peerAddress) {
        return confidentialMessageService.send(message, peerAddress);
    }

    public CompletableFuture<Connection> relay(Message message, Address peerAddress) {
        return confidentialMessageService.relay(message, peerAddress);
    }

    public CompletableFuture<GossipResult> requestAddData(Message message) {
        return dataService.requestAddData(message);
    }

    public CompletableFuture<GossipResult> requestRemoveData(Message message) {
        return dataService.requestRemoveData(message);
    }

    public CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter) {
        return dataService.requestInventory(dataFilter);
    }

    public void addMessageListener(MessageListener messageListener) {
        confidentialMessageService.addMessageListener(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        confidentialMessageService.removeMessageListener(messageListener);
    }

    public void shutdown() {
        dataService.shutdown();
        peerManager.shutdown();
        confidentialMessageService.shutdown();
        protectedNode.shutdown();
        capabilityAwareNode.shutdown();
        endPoint.shutdown();
        networkProxy.shutdown();
        storage.shutdown();
    }

    public Optional<Address> getAddress() {
        return protectedNode.getMyAddress();
    }

    @VisibleForTesting
    public PeerManager getPeerManager() {
        return peerManager;
    }
}
