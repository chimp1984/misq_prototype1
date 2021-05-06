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
import misq.p2p.proxy.ServerInfo;
import misq.p2p.router.gossip.GossipResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * High level API for the p2p network.
 */
public class P2pNetworkService {
    private static final Logger log = LoggerFactory.getLogger(P2pNetworkService.class);

    private final Storage storage;
    private final Map<NetworkType, Node> nodes = new ConcurrentHashMap<>();
    private final Map<NetworkType, Guard> guards = new ConcurrentHashMap<>();
    private final Map<NetworkType, ConfidentialMessageService> confidentialMessageServices = new ConcurrentHashMap<>();
    private final Map<NetworkType, DataService> dataServices = new ConcurrentHashMap<>();

    public P2pNetworkService(P2pNetworkConfig p2PNetworkConfig) {
        storage = new Storage();
        Set<NetworkType> mySupportedNetworks = p2PNetworkConfig.getNetworkConfigs().stream()
                .map(NetworkConfig::getNetworkType)
                .collect(Collectors.toSet());
        p2PNetworkConfig.getNetworkConfigs().forEach(networkConfig -> {
            NetworkType networkType = networkConfig.getNetworkType();

            Node node = new Node(networkConfig);
            CapabilityExchange capabilityExchange = new CapabilityExchange(node, mySupportedNetworks);
            Guard guard = new Guard(capabilityExchange);
            guards.put(networkType, guard);

            ConfidentialMessageService confidentialMessageService = new ConfidentialMessageService(guard);
            confidentialMessageServices.put(networkType, confidentialMessageService);

            DataService dataService = new DataService(guard, storage);
            dataServices.put(networkType, dataService);
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void bootstrap(Consumer<ServerInfo> resultHandler) {
        guards.values().forEach(guardedNode -> {
            guardedNode.bootstrap()
                    .whenComplete((serverInfo, throwable) -> {
                        if (serverInfo != null) {
                            resultHandler.accept(serverInfo);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    public void confidentialSend(Message message,
                                 Address peerAddress,
                                 Consumer<Connection> resultHandler) {
        NetworkType networkType = peerAddress.getNetworkType();
        if (confidentialMessageServices.containsKey(networkType)) {
            confidentialMessageServices.get(networkType)
                    .send(message, peerAddress)
                    .whenComplete((connection, throwable) -> {
                        if (connection != null) {
                            resultHandler.accept(connection);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        } else {
            confidentialMessageServices.values().forEach(service -> {
                service.relay(message, peerAddress)
                        .whenComplete((connection, throwable) -> {
                            if (connection != null) {
                                resultHandler.accept(connection);
                            } else {
                                log.error(throwable.toString());
                            }
                        });
            });
        }
    }

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

    public void addMessageListener(MessageListener messageListener) {
        confidentialMessageServices.values().forEach(service -> {
            service.addMessageListener(messageListener);
        });
    }

    public void removeMessageListener(MessageListener messageListener) {
        confidentialMessageServices.values().forEach(service -> {
            service.removeMessageListener(messageListener);
        });
    }

    public void shutdown() {
        confidentialMessageServices.values().forEach(ConfidentialMessageService::shutdown);
        dataServices.values().forEach(DataService::shutdown);
        guards.values().forEach(Guard::shutdown);
        storage.shutdown();
    }
}
