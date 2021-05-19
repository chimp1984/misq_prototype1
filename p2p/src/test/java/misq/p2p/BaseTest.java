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
import misq.p2p.data.storage.Storage;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Slf4j
public abstract class BaseTest {

    protected final Storage storage = new Storage();
    protected P2pNode alice, bob, carol;

    protected abstract int getTimeout();

    protected abstract Set<NetworkType> getMySupportedNetworks();

    protected abstract NetworkConfig getNetworkConfig(Config.Role role);

    protected abstract Address getPeerAddress(Config.Role role);

    protected void testBootstrapSolo(int count) throws InterruptedException {
        alice = new P2pNode(getNetworkConfig(Config.Role.Alice), getMySupportedNetworks(), storage, Config.aliceKeyRepository1);
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

    protected void testInitializeServer(int serversReadyLatchCount) throws InterruptedException {
        testInitializeServer(serversReadyLatchCount,
                getNetworkConfig(Config.Role.Alice), getNetworkConfig(Config.Role.Bob),
                getMySupportedNetworks(), getMySupportedNetworks());
    }

    protected void testInitializeServer(int serversReadyLatchCount,
                                        NetworkConfig networkConfigAlice,
                                        NetworkConfig networkConfigBob,
                                        Set<NetworkType> supportedNetworkTypesAlice,
                                        Set<NetworkType> supportedNetworkTypesBob) throws InterruptedException {
        alice = new P2pNode(networkConfigAlice, supportedNetworkTypesAlice, storage, Config.aliceKeyRepository1);
        bob = new P2pNode(networkConfigBob, supportedNetworkTypesBob, storage, Config.bobKeyRepository1);
        CountDownLatch serversReadyLatch = new CountDownLatch(serversReadyLatchCount);
        alice.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    protected void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        testInitializeServer(2);
        String msg = "hello";
        CountDownLatch receivedLatch = new CountDownLatch(1);
        bob.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            assertEquals(((MockMessage) message).getMsg(), msg);
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(1);

        Address peerAddress = getPeerAddress(Config.Role.Bob);
        alice.confidentialSend(new MockMessage(msg), peerAddress, Config.keyPairBob1.getPublic(), Config.keyPairAlice1)
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

    protected void startOfMultipleIds(NetworkType networkType, Set<NetworkType> mySupportedNetworks) throws InterruptedException {
        List<NetworkType> networkTypes = new ArrayList<>(mySupportedNetworks);

        String baseDirNameAlice = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Alice";
        NetworkId networkIdAlice1 = new NetworkId(baseDirNameAlice, "id_alice_1", 1111, networkTypes);
        alice = new P2pNode(new NetworkConfig(networkIdAlice1, networkType), mySupportedNetworks, storage, Config.aliceKeyRepository1);

        NetworkId networkIdAlice2 = new NetworkId(baseDirNameAlice, "id_alice_2", 1112, networkTypes);
        P2pNode alice2 = new P2pNode(new NetworkConfig(networkIdAlice2, networkType), mySupportedNetworks, storage, Config.aliceKeyRepository1);

        String baseDirNameBob = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Bob";
        NetworkId networkIdBob1 = new NetworkId(baseDirNameBob, "id_bob_1", 2222, networkTypes);
        bob = new P2pNode(new NetworkConfig(networkIdBob1, networkType), mySupportedNetworks, storage, Config.bobKeyRepository1);

        CountDownLatch serversReadyLatch = new CountDownLatch(3);
        alice.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        alice2.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    protected void sendMsgWithMultipleIds(NetworkType networkType,
                                          Set<NetworkType> mySupportedNetworks)
            throws InterruptedException, GeneralSecurityException {
        List<NetworkType> networkTypes = new ArrayList<>(mySupportedNetworks);
        String baseDirNameAlice = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Alice";
        NetworkId networkIdAlice1 = new NetworkId(baseDirNameAlice, "id_alice_1", 1111, networkTypes);
        P2pNode alice1 = new P2pNode(new NetworkConfig(networkIdAlice1, networkType), mySupportedNetworks, storage, Config.aliceKeyRepository1);

        NetworkId networkIdAlice2 = new NetworkId(baseDirNameAlice, "id_alice_2", 1112, networkTypes);
        P2pNode alice2 = new P2pNode(new NetworkConfig(networkIdAlice2, networkType), mySupportedNetworks, storage, Config.aliceKeyRepository2);

        String baseDirNameBob = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_Bob";
        NetworkId networkIdBob1 = new NetworkId(baseDirNameBob, "id_bob_1", 2222, networkTypes);
        P2pNode bob1 = new P2pNode(new NetworkConfig(networkIdBob1, networkType), mySupportedNetworks, storage, Config.bobKeyRepository1);

        NetworkId networkIdBob2 = new NetworkId(baseDirNameAlice, "id_bob_2", 2223, networkTypes);
        P2pNode bob2 = new P2pNode(new NetworkConfig(networkIdBob2, networkType), mySupportedNetworks, storage, Config.bobKeyRepository2);

        CountDownLatch serversReadyLatch = new CountDownLatch(4);
        alice1.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        alice2.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob1.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob2.initializeServer().whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
        String alice1ToBob1Msg = "alice1ToBob1Msg";
        String alice1ToBob2Msg = "alice1ToBob2Msg";
        String alice2ToBob1Msg = "alice2ToBob1Msg";
        String alice2ToBob2Msg = "alice2ToBob2Msg";
        String bob1ToAlice1Msg = "bob1ToAlice1Msg";
        String bob1ToAlice2Msg = "bob1ToAlice2Msg";
        String bob2ToAlice1Msg = "bob2ToAlice1Msg";
        String bob2ToAlice2Msg = "bob2ToAlice2Msg";

        CountDownLatch receivedLatch = new CountDownLatch(8);
        alice1.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("alice1 {} {}", message.toString(), connection);
            if (connection.getPeerAddress().equals(bob1.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), bob1ToAlice1Msg);
            } else if (connection.getPeerAddress().equals(bob2.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), bob2ToAlice1Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });
        alice2.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("alice2 {} {}", message.toString(), connection);
            if (connection.getPeerAddress().equals(bob1.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), bob1ToAlice2Msg);
            } else if (connection.getPeerAddress().equals(bob2.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), bob2ToAlice2Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });

        bob1.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("bob1 {} {}", message.toString(), connection);
            if (connection.getPeerAddress().equals(alice1.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), alice1ToBob1Msg);
            } else if (connection.getPeerAddress().equals(alice2.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), alice2ToBob1Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });
        bob2.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            log.error("bob2 {} {}", message.toString(), connection);
            if (connection.getPeerAddress().equals(alice1.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), alice1ToBob2Msg);
            } else if (connection.getPeerAddress().equals(alice2.getAddress().get())) {
                assertEquals(((MockMessage) message).getMsg(), alice2ToBob2Msg);
            } else {
                fail();
            }
            receivedLatch.countDown();
        });

        CountDownLatch sentLatch = new CountDownLatch(8);
        alice1.confidentialSend(new MockMessage(alice1ToBob1Msg), bob1.getAddress().get(), Config.keyPairBob1.getPublic(), Config.keyPairAlice1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        alice1.confidentialSend(new MockMessage(alice1ToBob2Msg), bob2.getAddress().get(), Config.keyPairBob2.getPublic(), Config.keyPairAlice1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        alice2.confidentialSend(new MockMessage(alice2ToBob1Msg), bob1.getAddress().get(), Config.keyPairBob1.getPublic(), Config.keyPairAlice2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        alice2.confidentialSend(new MockMessage(alice2ToBob2Msg), bob2.getAddress().get(), Config.keyPairBob2.getPublic(), Config.keyPairAlice2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        bob1.confidentialSend(new MockMessage(bob1ToAlice1Msg), alice1.getAddress().get(), Config.keyPairAlice1.getPublic(), Config.keyPairBob1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob1.confidentialSend(new MockMessage(bob1ToAlice2Msg), alice2.getAddress().get(), Config.keyPairAlice2.getPublic(), Config.keyPairBob1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob2.confidentialSend(new MockMessage(bob2ToAlice1Msg), alice1.getAddress().get(), Config.keyPairAlice1.getPublic(), Config.keyPairBob2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });
        bob2.confidentialSend(new MockMessage(bob2ToAlice2Msg), alice2.getAddress().get(), Config.keyPairAlice2.getPublic(), Config.keyPairBob2)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        boolean allSent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(allSent);
        boolean allReceived = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(allReceived);
    }

}
