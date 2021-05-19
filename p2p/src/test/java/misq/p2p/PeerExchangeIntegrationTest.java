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

package misq.p2p;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import misq.common.util.OsUtils;
import misq.common.util.Tuple2;
import misq.p2p.data.storage.Storage;
import misq.p2p.node.Node;
import misq.p2p.peers.PeerConfig;
import misq.p2p.peers.PeerGroup;
import misq.p2p.peers.exchange.DefaultPeerExchangeStrategy;
import misq.p2p.peers.exchange.PeerExchangeConfig;
import misq.p2p.peers.exchange.PeerExchangeManager;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class PeerExchangeIntegrationTest {
    private PeerExchangeManager seed, node1, node2, node3, node4;
    private PeerGroup peerGroupSeed, peerGroupNode1, peerGroupNode2, peerGroupNode3, peerGroupNode4;
    private Set<NetworkType> mySupportedNetworks = Sets.newHashSet(NetworkType.CLEAR);
    private Storage storage = new Storage();
    private List<P2pNode> nodes;
    private int numSeeNodesAtBoostrap = 1;
    private int numPersistedPeersAtBoostrap = 8;
    private int numReportedPeersAtBoostrap = 20;
    private int minNumReportedPeers = 20;
    private int minNumConnectedPeers = 30;
    private int maxNumConnectedPeers = 40;
    private int repeatPeerExchangeDelay = 200;
    private Map<Integer, Tuple2<PeerExchangeManager, PeerGroup>> tuples;


    @Test
    public void testPeerExchange() throws InterruptedException {
      /*  bootstrapSeedNode();
        shutDownSeed();*/

      /*  bootstrapSeedNode();
        bootstrapNode1();
        shutDownSeed();
        shutDownNode1();*/

      /*  bootstrapSeedNode();
        bootstrapNode1();
        bootstrapNode2();
        shutDownSeed();
        shutDownNode1();
        shutDownNode2();*/

        bootstrapSeedNode();
        bootstrapNode1();
        bootstrapNode2();
        bootstrapNode3();
        shutDownSeed();
        shutDownNode1();
        shutDownNode2();
        shutDownNode3();

     /*   bootstrapSeedNode();
        bootstrapNodes(5);*/
       /* shutDownSeed();
        shutDownNode1();*/

    }

    // Seed node only, so num connection and num reported will be 0
    protected void bootstrapSeedNode() throws InterruptedException {
        NetworkConfig networkConfig = getNetworkConfig(1000);
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(networkConfig).whenComplete((tuple, e) -> {
            seed = tuple.first;
            peerGroupSeed = tuple.second;

            log.info("bootstrap seed");
            seed.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("seed bootstrapped");
                            latch.countDown();
                            assertEquals(0, peerGroupSeed.getConnections().size());
                            assertEquals(0, peerGroupSeed.getReportedPeers().size());
                        }
                    });
        });
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);
    }

    /*
     * n1 []-> s []-> n1
     */
    protected void bootstrapNode1() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(getNetworkConfig(2001)).whenComplete((tuple, e) -> {
            node1 = tuple.first;
            peerGroupNode1 = tuple.second;
            log.info("bootstrap node1");
            // n1->s
            node1.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("node1 bootstrapped");

                            latch.countDown();
                            assertEquals(1, peerGroupNode1.getConnections().size());
                            assertEquals(0, peerGroupNode1.getReportedPeers().size());

                            assertEquals(1, peerGroupSeed.getConnections().size());
                            assertEquals(0, peerGroupSeed.getReportedPeers().size());
                        }
                    });
        });

        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);
    }

    /*
     * n1 []-> s []-> n1
     * n2 []-> s [n1]-> n2
     * n2 []-> n1 []-> n2
     */
    protected void bootstrapNode2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(getNetworkConfig(2002))
                .whenComplete((tuple, e) -> {
                    node2 = tuple.first;
                    peerGroupNode2 = tuple.second;
                    log.info("bootstrap node2");
                    // n2->s
                    node2.bootstrap()
                            .whenComplete((success, t) -> {
                                if (success && t == null) {
                                    log.info("node2 bootstrapped");
                                    latch.countDown();
                                    assertEquals(1, peerGroupNode2.getConnections().size());
                                    assertEquals(1, peerGroupNode2.getReportedPeers().size());

                                    assertEquals(1, peerGroupNode1.getConnections().size());
                                    assertEquals(0, peerGroupNode1.getReportedPeers().size());

                                    assertEquals(2, peerGroupSeed.getConnections().size());
                                    assertEquals(0, peerGroupSeed.getReportedPeers().size());
                                }
                            });
                });
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        Set<Integer> reportedPeerPorts = peerGroupNode2.getReportedPeers().stream()
                .map(peer -> peer.getAddress().getPort()).collect(Collectors.toSet());
        assertTrue(reportedPeerPorts.contains(2001));

        // n2->n1
        node2.bootstrap()
                .whenComplete((success, t) -> {
                    if (success && t == null) {
                        log.info("node2 bootstrapped again");
                        latch.countDown();
                        assertEquals(2, peerGroupNode2.getConnections().size());
                        assertEquals(1, peerGroupNode2.getReportedPeers().size());

                        assertEquals(2, peerGroupNode1.getConnections().size());
                        assertEquals(0, peerGroupNode1.getReportedPeers().size());

                        assertEquals(2, peerGroupSeed.getConnections().size());
                        assertEquals(0, peerGroupSeed.getReportedPeers().size());
                    }
                });
    }

    /*
     * n1 []-> s []-> n1
     * n2 []-> s [n1]-> n2
     * n2 []-> n1 []-> n2
     *
     * n3 []-> s [n1, n2]-> n3
     * n3 [n2]-> n1 [n2]-> n3
     * n3 [n1]-> n2 [n1]-> n3
     */
    protected void bootstrapNode3() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        getTuple(getNetworkConfig(2003)).whenComplete((tuple, e) -> {
            node3 = tuple.first;
            peerGroupNode3 = tuple.second;
            log.info("bootstrap node3");
            // n3 -> s
            node3.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("node3 bootstrapped");
                            latch.countDown();
                            assertEquals(1, peerGroupNode3.getConnections().size());
                            assertEquals(2, peerGroupNode3.getReportedPeers().size());

                            assertEquals(2, peerGroupNode2.getConnections().size());
                            assertEquals(1, peerGroupNode2.getReportedPeers().size());

                            assertEquals(2, peerGroupNode1.getConnections().size());
                            assertEquals(0, peerGroupNode1.getReportedPeers().size());

                            assertEquals(3, peerGroupSeed.getConnections().size());
                            assertEquals(0, peerGroupSeed.getReportedPeers().size());
                        }
                    });
        });
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        Set<Integer> reportedPeerPorts = peerGroupNode3.getReportedPeers().stream()
                .map(peer -> peer.getAddress().getPort()).collect(Collectors.toSet());
        assertTrue(reportedPeerPorts.contains(2001));
        assertTrue(reportedPeerPorts.contains(2002));

        // n3-> n1, n3-> n2
        node3.bootstrap()
                .whenComplete((success, t) -> {
                    if (success && t == null) {
                        log.info("node2 bootstrapped again");
                        latch.countDown();
                        assertEquals(2, peerGroupNode2.getConnections().size());
                        assertEquals(1, peerGroupNode2.getReportedPeers().size());

                        assertEquals(2, peerGroupNode1.getConnections().size());
                        assertEquals(0, peerGroupNode1.getReportedPeers().size());

                        assertEquals(2, peerGroupSeed.getConnections().size());
                        assertEquals(0, peerGroupSeed.getReportedPeers().size());
                    }
                });
    }

    protected void bootstrapNodes(int numNodes) throws InterruptedException {
        numReportedPeersAtBoostrap = 20;
        minNumReportedPeers = 30;
        tuples = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(numNodes);
        for (int i = 0; i < numNodes; i++) {
            int counter = i;
            getTuple(getNetworkConfig(2000 + counter)).whenComplete((tuple, e) -> {
                tuples.put(counter, tuple);
                PeerExchangeManager node = tuple.first;
                log.info("bootstrap node {}", counter);
                node.bootstrap()
                        .whenComplete((success, t) -> {
                            if (success && t == null) {
                                log.info("Bootstrap completed: node {}", counter);
                                latch.countDown();
                            }
                        });

                CountDownLatch latchRepeat = new CountDownLatch(tuples.size() - 1);
                for (int j = 0; j < tuples.size(); j++) {
                   /* if (j == 0) {
                        continue;
                    }*/
                    int finalJ = j;
                    tuples.get(j).first.bootstrap()
                            .whenComplete((success, t) -> {
                                if (success && t == null) {
                                    log.info("Repeated bootstrap completed: node {}", finalJ);
                                    latchRepeat.countDown();
                                }
                            });
                }
                try {
                    boolean repeatedBootstrapped = latchRepeat.await(5, TimeUnit.SECONDS);
                    assertTrue(repeatedBootstrapped);
                } catch (InterruptedException ignore) {
                }
            });

            // Make sequence predictable
            Thread.sleep(50);
        }
        boolean bootstrapped = latch.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        CountDownLatch seedRepeat = new CountDownLatch(1);
        seed.bootstrap().whenComplete((s, e) -> {
            seedRepeat.countDown();
        });
        boolean seedRepeatRepeatedBootstrapped = seedRepeat.await(5, TimeUnit.SECONDS);
        assertTrue(seedRepeatRepeatedBootstrapped);


        tuples.forEach((key, value) -> {
            PeerGroup peerGroup = value.second;
            log.error("node 200{}, numConnection={}, numReported={}", key, peerGroup.getConnections(), peerGroup.getReportedPeers().size());
        });
        Thread.sleep(100);

       /* CountDownLatch latch2 = new CountDownLatch(1);
        tuples.get(1).first.bootstrap()
                .whenComplete((success, t) -> {
                    if (success && t == null) {
                        log.info("node2 bootstrapped again");
                        latch2.countDown();
                    }
                });
*/

      /*  for (int i = 0; i < numNodes; i++) {
            Tuple2<PeerExchangeManager, PeerGroup> tuple = tuples.get(i);
            PeerExchangeManager node = tuple.first;
            log.info("Repeated bootstrap node {}", i);
            int c = i;
            node.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("Repeated bootstrap completed: node {} ", c);
                            latch2.countDown();
                        }
                    });

            // Make sequence predictable
            Thread.sleep(50);
        }*/
      /*  boolean bootstrapped2 = latch2.await(5, TimeUnit.SECONDS);
        assertTrue(bootstrapped2);*/
        tuples.forEach((key, value) -> {
            PeerGroup peerGroup = value.second;
            log.error("Repeated: node 200{}, numConnection={}, numReported={}", key, peerGroup.getConnections().size(), peerGroup.getReportedPeers().size());
        });

    }

    private CompletableFuture<Tuple2<PeerExchangeManager, PeerGroup>> getTuple(NetworkConfig networkConfig) {
        Node node = new Node(networkConfig);

        PeerConfig peerConfig = networkConfig.getPeerConfig();
        PeerGroup peerGroup = new PeerGroup(node, peerConfig, networkConfig.getNodeId().getServerPort());
        DefaultPeerExchangeStrategy peerExchangeStrategy = new DefaultPeerExchangeStrategy(peerGroup, peerConfig);
        return node.initializeServer(networkConfig.getNodeId().getId(), networkConfig.getNodeId().getServerPort())
                .thenApply(e -> new Tuple2<>(new PeerExchangeManager(node, peerExchangeStrategy), peerGroup));
    }

    protected void shutDownSeed() {
        seed.shutdown();
    }

    protected void shutDownNode1() {
        node1.shutdown();
    }

    protected void shutDownNode2() {
        node2.shutdown();
    }

    protected void shutDownNode3() {
        node3.shutdown();
    }

    protected void shutDownNode4() {
        node4.shutdown();
    }

    protected void shutDownNodes() {
        nodes.forEach(P2pNode::shutdown);
    }

    protected NetworkConfig getNetworkConfig(int serverPort,
                                             List<Address> seedNodes,
                                             int repeatPeerExchangeDelay,
                                             int minNumConnectedPeers,
                                             int maxNumConnectedPeers) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_node_" + serverPort;
        PeerExchangeConfig peerExchangeConfig = new PeerExchangeConfig(numSeeNodesAtBoostrap,
                numPersistedPeersAtBoostrap,
                numReportedPeersAtBoostrap,
                repeatPeerExchangeDelay);
        PeerConfig peerConfig = new PeerConfig(peerExchangeConfig,
                seedNodes,
                minNumConnectedPeers,
                maxNumConnectedPeers,
                minNumReportedPeers);

        NodeId nodeId = new NodeId("default", serverPort, Sets.newHashSet(NetworkType.CLEAR));
        return new NetworkConfig(baseDirName, nodeId, NetworkType.CLEAR, peerConfig);
    }

    protected NetworkConfig getNetworkConfig(int serverPort) {
        List<Address> seedNodes = Arrays.asList(Address.localHost(1000));
        return getNetworkConfig(serverPort,
                seedNodes,
                repeatPeerExchangeDelay,
                minNumConnectedPeers,
                maxNumConnectedPeers);
    }
}
