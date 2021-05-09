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


import misq.p2p.capability.CapabilityExchange;
import misq.p2p.confidential.ConfidentialMessageService;
import misq.p2p.data.DataService;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.data.storage.Storage;
import misq.p2p.guard.Guard;
import misq.p2p.node.*;
import misq.p2p.peers.PeerGroup;
import misq.p2p.peers.PeerManager;
import misq.p2p.peers.exchange.PeerExchange;
import misq.p2p.peers.exchange.PeerExchangeSelection;
import misq.p2p.proxy.ServerInfo;
import misq.p2p.router.gossip.GossipResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * High level API for the p2p network.
 */
public class P2pServiceImpl implements P2pService {
    private static final Logger log = LoggerFactory.getLogger(P2pServiceImpl.class);

    private final Storage storage;
    private final Map<NetworkType, Node> nodes = new ConcurrentHashMap<>();
    private final Map<NetworkType, PeerManager> peerManagers = new ConcurrentHashMap<>();
    private final Map<NetworkType, Guard> guards = new ConcurrentHashMap<>();
    private final Map<NetworkType, ConfidentialMessageService> confidentialMessageServices = new ConcurrentHashMap<>();
    private final Map<NetworkType, DataService> dataServices = new ConcurrentHashMap<>();

    public P2pServiceImpl(List<NetworkConfig> networkConfigs) {
        storage = new Storage();
        Set<NetworkType> mySupportedNetworks = networkConfigs.stream()
                .map(NetworkConfig::getNetworkType)
                .collect(Collectors.toSet());
        networkConfigs.forEach(networkConfig -> {
            NetworkType networkType = networkConfig.getNetworkType();

            Node node = new Node(networkConfig);
            CapabilityExchange capabilityExchange = new CapabilityExchange(node, mySupportedNetworks);
            Guard guard = new Guard(capabilityExchange);
            guards.put(networkType, guard);

            PeerGroup peerGroup = new PeerGroup(guard, networkConfig.getPeerConfig());
            PeerExchangeSelection peerExchangeSelection = new PeerExchangeSelection(peerGroup, networkConfig.getPeerConfig());
            PeerExchange peerExchange = new PeerExchange(guard, peerExchangeSelection);
            PeerManager peerManager = new PeerManager(guard, peerExchange);
            peerManagers.put(networkType, peerManager);

            ConfidentialMessageService confidentialMessageService = new ConfidentialMessageService(guard, peerGroup);
            confidentialMessageServices.put(networkType, confidentialMessageService);

            DataService dataService = new DataService(guard, peerGroup, storage);
            dataServices.put(networkType, dataService);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void initializeServer(BiConsumer<ServerInfo, Throwable> resultHandler) {
        guards.values().forEach(guard -> {
            guard.initializeServer()
                    .whenComplete((serverInfo, throwable) -> {
                        if (serverInfo != null) {
                            resultHandler.accept(serverInfo, null);
                        } else {
                            log.error(throwable.toString(), throwable);
                            resultHandler.accept(null, throwable);
                        }
                    });
        });
    }

    @Override
    public void bootstrap(BiConsumer<Boolean, Throwable> resultHandler) {
        peerManagers.values().forEach(peerManager -> {
            peerManager.bootstrap()
                    .whenComplete((success, throwable) -> {
                        if (success) {
                            resultHandler.accept(success, null);
                        } else {
                            log.error(throwable.toString(), throwable);
                            resultHandler.accept(null, throwable);
                        }
                    });
        });
    }

    @Override
    public CompletableFuture<Connection> confidentialSend(Message message, Address peerAddress) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        NetworkType networkType = peerAddress.getNetworkType();
        if (confidentialMessageServices.containsKey(networkType)) {
            confidentialMessageServices.get(networkType)
                    .send(message, peerAddress)
                    .whenComplete((connection, throwable) -> {
                        if (connection != null) {
                            future.complete(connection);
                        } else {
                            log.error(throwable.toString(), throwable);
                            future.completeExceptionally(throwable);
                        }
                    });
        } else {
            confidentialMessageServices.values().forEach(service -> {
                service.relay(message, peerAddress)
                        .whenComplete((connection, throwable) -> {
                            if (connection != null) {
                                future.complete(connection);
                            } else {
                                log.error(throwable.toString(), throwable);
                                future.completeExceptionally(throwable);
                            }
                        });
            });
        }
        return future;
    }

    @Override
    public void requestAddData(Message message,
                               Consumer<GossipResult> resultHandler) {
        dataServices.values().forEach(dataService -> {
            dataService.requestAddData(message)
                    .whenComplete((gossipResult, throwable) -> {
                        if (gossipResult != null) {
                            resultHandler.accept(gossipResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    @Override
    public void requestRemoveData(Message message,
                                  Consumer<GossipResult> resultHandler) {
        dataServices.values().forEach(dataService -> {
            dataService.requestRemoveData(message)
                    .whenComplete((gossipResult, throwable) -> {
                        if (gossipResult != null) {
                            resultHandler.accept(gossipResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    @Override
    public void requestInventory(DataFilter dataFilter,
                                 Consumer<RequestInventoryResult> resultHandler) {
        dataServices.values().forEach(dataService -> {
            dataService.requestInventory(dataFilter)
                    .whenComplete((requestInventoryResult, throwable) -> {
                        if (requestInventoryResult != null) {
                            resultHandler.accept(requestInventoryResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    @Override
    public void addMessageListener(MessageListener messageListener) {
        confidentialMessageServices.values().forEach(service -> {
            service.addMessageListener(messageListener);
        });
    }

    @Override
    public void removeMessageListener(MessageListener messageListener) {
        confidentialMessageServices.values().forEach(service -> {
            service.removeMessageListener(messageListener);
        });
    }

    @Override
    public void shutdown() {
        confidentialMessageServices.values().forEach(ConfidentialMessageService::shutdown);
        dataServices.values().forEach(DataService::shutdown);
        peerManagers.values().forEach(PeerManager::shutdown);
        guards.values().forEach(Guard::shutdown);
        storage.shutdown();
    }

    @Override
    public Optional<Address> getAddress(NetworkType networkType) {
        return guards.get(networkType).getMyAddress();
    }
}