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


import misq.common.util.CollectionUtil;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.data.storage.Storage;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.MessageListener;
import misq.p2p.node.proxy.GetServerSocketResult;
import misq.p2p.router.gossip.GossipResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * High level API for the p2p network.
 */
public class P2pService {
    private static final Logger log = LoggerFactory.getLogger(P2pService.class);

    private final Map<NetworkType, P2pNode> p2pNodes = new ConcurrentHashMap<>();

    public P2pService(Set<NetworkConfig> networkConfigs, KeyPairRepository keyPairRepository) {
        Storage storage = new Storage("");//todo
        networkConfigs.forEach(networkConfig -> {
            NetworkType networkType = networkConfig.getNetworkType();
            P2pNode p2pNode = new P2pNode(networkConfig, storage, keyPairRepository);
            p2pNodes.put(networkType, p2pNode);
        });
    }

    public P2pService() {
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initializeServer(BiConsumer<GetServerSocketResult, Throwable> resultHandler) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        int numNodes = p2pNodes.size();
        p2pNodes.values().forEach(p2pNode -> {
            p2pNode.initializeServer()
                    .whenComplete((serverInfo, throwable) -> {
                        if (serverInfo != null) {
                            resultHandler.accept(serverInfo, null);
                            int compl = completed.incrementAndGet();
                            if (compl + failed.get() == numNodes) {
                                future.complete(compl == numNodes);
                            }
                        } else {
                            log.error(throwable.toString(), throwable);
                            resultHandler.accept(null, throwable);
                            if (failed.incrementAndGet() + completed.get() == numNodes) {
                                future.complete(false);
                            }
                        }
                    });
        });
        return future;
    }

    public CompletableFuture<Boolean> bootstrap() {
        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();
        p2pNodes.values().forEach(p2pNode -> {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            allFutures.add(future);
            p2pNode.bootstrap()
                    .whenComplete((success, e) -> {
                        if (e == null) {
                            future.complete(success); // Can be still false
                        } else {
                            future.complete(false);
                        }
                    });
        });
        return CollectionUtil.allOf(allFutures)                                 // We require all futures the be completed
                .thenApply(resultList -> resultList.stream().anyMatch(e -> e))  // If at least one network succeeded
                .thenCompose(CompletableFuture::completedFuture);               // If at least one was successful we report a success
    }

    public CompletableFuture<Connection> confidentialSend(Message message, Set<Address> peerAddresses,
                                                          PublicKey peersPublicKey, KeyPair myKeyPair) {
        CompletableFuture<Connection> future = new CompletableFuture<>();
        peerAddresses.forEach(peerAddress -> {
            try {
                NetworkType networkType = peerAddress.getNetworkType();
                if (p2pNodes.containsKey(networkType)) {
                    p2pNodes.get(networkType)
                            .confidentialSend(message, peerAddress, peersPublicKey, myKeyPair)
                            .whenComplete((connection, throwable) -> {
                                if (connection != null) {
                                    future.complete(connection);
                                } else {
                                    log.error(throwable.toString(), throwable);
                                    future.completeExceptionally(throwable);
                                }
                            });
                } else {
                    p2pNodes.values().forEach(p2pNode -> {
                        p2pNode.relay(message, peerAddress)
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
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        });
        return future;
    }

    public void requestAddData(Message message, Consumer<GossipResult> resultHandler) {
        p2pNodes.values().forEach(p2pNode -> {
            p2pNode.requestAddData(message)
                    .whenComplete((gossipResult, throwable) -> {
                        if (gossipResult != null) {
                            resultHandler.accept(gossipResult);
                        } else {
                            log.error(throwable.toString());
                        }
                    });
        });
    }

    public void requestRemoveData(Message message, Consumer<GossipResult> resultHandler) {
        p2pNodes.values().forEach(dataService -> {
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

    public void requestInventory(DataFilter dataFilter, Consumer<RequestInventoryResult> resultHandler) {
        p2pNodes.values().forEach(p2pNode -> {
            p2pNode.requestInventory(dataFilter)
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
        p2pNodes.values().forEach(p2pNode -> {
            p2pNode.addMessageListener(messageListener);
        });
    }

    public void removeMessageListener(MessageListener messageListener) {
        p2pNodes.values().forEach(p2pNode -> {
            p2pNode.removeMessageListener(messageListener);
        });
    }

    public void shutdown() {
        p2pNodes.values().forEach(P2pNode::shutdown);
    }

    public Optional<Address> findMyAddress(NetworkType networkType) {
        return p2pNodes.get(networkType).findMyAddress();
    }

    public Set<Address> findMyAddresses() {
        return p2pNodes.values().stream()
                .flatMap(p2pNode -> p2pNode.findMyAddress().stream())
                .collect(Collectors.toSet());
    }
}
