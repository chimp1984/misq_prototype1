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

import lombok.extern.slf4j.Slf4j;
import misq.common.util.OsUtils;
import misq.p2p.node.Address;
import misq.p2p.node.Node;
import misq.p2p.peers.PeerConfig;
import misq.p2p.peers.PeerGroup;
import misq.p2p.peers.PeerManager;
import misq.p2p.peers.exchange.PeerExchangeConfig;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@Slf4j
public class BaseTest {
    private P2pServiceImpl seed, node1, node2, node3, node4;
    private PeerGroup peerGroupSeed, peerGroupNode1, peerGroupNode2, peerGroupNode3, peerGroupNode4;

    protected enum Role {
        Alice,
        Bob,
        Carol
    }

    protected P2pServiceImpl alice;
    protected P2pServiceImpl bob;

    protected int getTimeout() {
        return 10;
    }

    protected List<NetworkConfig> getNetworkConfig(Role role) {
        int serverPort;
        switch (role) {
            case Alice:
                serverPort = 1111;
                break;
            case Bob:
                serverPort = 2222;
                break;
            case Carol:
            default:
                serverPort = 3333;
                break;
        }

        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NetworkConfig clearNet = new NetworkConfig(baseDirName,
                NetworkType.CLEAR,
                Node.DEFAULT_SERVER_ID,
                serverPort);
        return List.of(clearNet);
    }

    protected List<NetworkConfig> getNetworkConfig(int serverPort,
                                                   List<Address> seedNodes,
                                                   int repeatPeerExchangeDelay,
                                                   int minNumConnectedPeers,
                                                   int maxNumConnectedPeers) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_node_" + serverPort;
        int numSeeNodesAtBoostrap = 2;
        int numPersistedPeersAtBoostrap = 8;
        int numReportedPeersAtBoostrap = 4;
        int minNumReportedPeers = 20;
        PeerExchangeConfig peerExchangeConfig = new PeerExchangeConfig(numSeeNodesAtBoostrap,
                numPersistedPeersAtBoostrap,
                numReportedPeersAtBoostrap,
                repeatPeerExchangeDelay);
        PeerConfig peerConfig = new PeerConfig(peerExchangeConfig,
                seedNodes,
                minNumConnectedPeers,
                maxNumConnectedPeers,
                minNumReportedPeers);
        NetworkConfig clearNet = new NetworkConfig(baseDirName,
                NetworkType.CLEAR,
                Node.DEFAULT_SERVER_ID,
                serverPort,
                peerConfig);
        return List.of(clearNet);
    }

    protected void testBootstrapSolo(int count) throws InterruptedException {
        alice = new P2pServiceImpl(getNetworkConfig(Role.Alice));
        CountDownLatch bootstrappedLatch = new CountDownLatch(count);
        alice.bootstrap().whenComplete((success, t) -> {
            if (success && t == null) {
                bootstrappedLatch.countDown();
            }
        });

        boolean bootstrapped = bootstrappedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(bootstrapped);
        alice.shutdown();
    }


    protected List<NetworkConfig> getNetworkConfig(int serverPort) {
        int minNumConnectedPeers = 4;
        int maxNumConnectedPeers = 5;
        int repeatPeerExchangeDelay = 10;
        List<Address> seedNodes = Arrays.asList(Address.localHost(1000));
        return getNetworkConfig(serverPort,
                seedNodes,
                repeatPeerExchangeDelay,
                minNumConnectedPeers,
                maxNumConnectedPeers);
    }

    // Seed node only, so num connection and num reported will be 0
    protected void bootstrapSeedNode() throws InterruptedException {
        seed = new P2pServiceImpl(getNetworkConfig(1000));
        peerGroupSeed = seed.getPeerManager(NetworkType.CLEAR).getPeerGroup();

        CountDownLatch latch = new CountDownLatch(1);
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
        boolean bootstrapped = latch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(bootstrapped);
    }

    /*
     * n1 []-> s []-> n1
     */
    protected void bootstrapSeedNodeAndNode1() throws InterruptedException {
        bootstrapSeedNode();
        node1 = new P2pServiceImpl(getNetworkConfig(2000));
        peerGroupNode1 = node1.getPeerManager(NetworkType.CLEAR).getPeerGroup();
        log.info("bootstrap node1");

        CountDownLatch latch = new CountDownLatch(1);
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
        boolean bootstrapped = latch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        // We give time for nodes to initiate another exchange with the node1 and expect same results
        Thread.sleep(100);
        assertEquals(1, peerGroupNode1.getConnections().size());
        assertEquals(0, peerGroupNode1.getReportedPeers().size());

        assertEquals(1, peerGroupSeed.getConnections().size());
        assertEquals(0, peerGroupSeed.getReportedPeers().size());
    }

    /*
     * n1 []-> s []-> n1
     * n2 []-> s [n1]-> n2
     * n2 repeats:
     * n2 []-> n1 []-> n2
     */
    protected void bootstrapSeedNodeAndNode1AndNode2() throws InterruptedException {
        bootstrapSeedNodeAndNode1();

        node2 = new P2pServiceImpl(getNetworkConfig(3000));
        peerGroupNode2 = node2.getPeerManager(NetworkType.CLEAR).getPeerGroup();
        log.info("bootstrap node2");

        CountDownLatch latch = new CountDownLatch(1);
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
        boolean bootstrapped = latch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        // We give time for nodes to initiate another exchange with the node2 and expect
        // that node2 has 2 connections now (n2->n1 after having received n1 from seed as reported peer).
        // Also num connections at n2 has increased to 2 but reported peers is still 0 as the new inbound connection
        // has no new peer.
        Thread.sleep(100);
        assertEquals(2, peerGroupNode2.getConnections().size());
        assertEquals(1, peerGroupNode2.getReportedPeers().size());

        assertEquals(2, peerGroupNode1.getConnections().size());
        assertEquals(0, peerGroupNode1.getReportedPeers().size());

        assertEquals(2, peerGroupSeed.getConnections().size());
        assertEquals(0, peerGroupSeed.getReportedPeers().size());
    }


    /*
     * n1 []-> s []-> n1
     * n2 []-> s [n1]-> n2
     * n2 repeats:
     * n2 []-> n1 []-> n2
     * n3 []-> s [n1, n2]-> n3
     * n3 repeats:
     * n3 [n2]-> n1 [n2]-> n3  // we dont filter sent peers with received, so we send back n2
     * n3 [n1]-> n2 [n1]-> n3
     */
    protected void bootstrapSeedNodeAndNode1AndNode2AndNode3() throws InterruptedException {
        bootstrapSeedNodeAndNode1AndNode2();

        node3 = new P2pServiceImpl(getNetworkConfig(4000));
        peerGroupNode3 = node3.getPeerManager(NetworkType.CLEAR).getPeerGroup();
        log.info("bootstrap node3");

        CountDownLatch latch = new CountDownLatch(1);
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
        boolean bootstrapped = latch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(bootstrapped);

        Thread.sleep(100);
        assertEquals(3, peerGroupNode3.getConnections().size());
        assertEquals(2, peerGroupNode3.getReportedPeers().size());

        // reported peers for n1,n2 has not changes as n3 sent outbound msg and it is not added to reported as its the direct peer
        assertEquals(3, peerGroupNode2.getConnections().size());
        assertEquals(1, peerGroupNode2.getReportedPeers().size());

        assertEquals(3, peerGroupNode1.getConnections().size());
        assertEquals(1, peerGroupNode1.getReportedPeers().size());

        assertEquals(3, peerGroupSeed.getConnections().size());
        assertEquals(0, peerGroupSeed.getReportedPeers().size());
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


    protected void testBootstrap1NodeTo1Seed2() throws InterruptedException {
        int minNumConnectedPeers = 4;
        int maxNumConnectedPeers = 5;
        int repeatPeerExchangeDelay = 1000;
        List<Address> seedNodes = Arrays.asList(Address.localHost(1000));

        int numNodes = 2;
        Set<P2pService> p2pServices = new HashSet<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 1000; i < 1000 + numNodes; i++) {
            CountDownLatch peerGroupLatch = new CountDownLatch(1);
            List<NetworkConfig> networkConfig = getNetworkConfig(i, seedNodes, repeatPeerExchangeDelay, minNumConnectedPeers, maxNumConnectedPeers);
            P2pServiceImpl p2pService = new P2pServiceImpl(networkConfig);
            p2pServices.add(p2pService);
            log.info("Node {} created", i);
            int final_i = i;
            p2pService.bootstrap()
                    .whenComplete((success, t) -> {
                        if (success && t == null) {
                            log.info("Node {} bootstrapped", final_i);
                            peerGroupLatch.countDown();
                            PeerGroup peerGroup = p2pService.getPeerManager(NetworkType.CLEAR).getPeerGroup();
                            assertEquals(counter.get(), peerGroup.getConnections().size());
                            counter.incrementAndGet();
                        }
                    });
            boolean bootstrapped = peerGroupLatch.await(getTimeout(), TimeUnit.SECONDS);
            assertTrue(bootstrapped);
        }


        // give time for repeated peer exchange attempts
        Thread.sleep(100);

        p2pServices.forEach(p2pService -> {
            PeerManager peerManager = ((P2pServiceImpl) p2pService).getPeerManager(NetworkType.CLEAR);
            int numConnections = peerManager.getPeerGroup().getConnections().size();
            log.info("{}: numConnections {}", peerManager.getGuard().getMyAddress(), numConnections);
            //  assertTrue(numConnections >= minNumConnectedPeers);
        });

        p2pServices.forEach(P2pService::shutdown);
    }

    protected void testInitializeServer(int serversReadyLatchCount) throws InterruptedException {
        alice = new P2pServiceImpl(getNetworkConfig(Role.Alice));
        bob = new P2pServiceImpl(getNetworkConfig(Role.Bob));
        CountDownLatch serversReadyLatch = new CountDownLatch(serversReadyLatchCount);
        alice.initializeServer((serverInfo, throwable) -> {
            serversReadyLatch.countDown();
        });
        bob.initializeServer((serverInfo, throwable) -> {
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    protected void testConfidentialSend(NetworkType networkType) throws InterruptedException {
        alice = new P2pServiceImpl(getNetworkConfig(Role.Alice));
        bob = new P2pServiceImpl(getNetworkConfig(Role.Bob));
        String msg = "hello";
        CountDownLatch receivedLatch = new CountDownLatch(1);
        bob.addMessageListener((connection, message) -> {
            assertTrue(message instanceof MockMessage);
            assertEquals(((MockMessage) message).getMsg(), msg);
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(1);

        Optional<Address> address = bob.getAddress(networkType);
        assertTrue(address.isPresent());
        Address peerAddress = address.get();
        alice.confidentialSend(new MockMessage(msg), peerAddress)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        boolean sent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(sent);

        boolean received = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(received);
    }
}
