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
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Slf4j
public class AllNetworksIntegrationTest {
    private P2pServiceImpl alice, bob;

    private List<NetworkConfig> getNetNetworkConfigs(Config.Role role) {
        return List.of(Config.getClearNetNetworkConfig(role), Config.getTorNetworkConfig(role), Config.getI2pNetworkConfig(role));
    }

    // @Test
    public void testInitializeServer() throws InterruptedException {
        try {
            initializeServer();
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }

    public void initializeServer() throws InterruptedException {
        alice = new P2pServiceImpl(getNetNetworkConfigs(Config.Role.Alice), Config.aliceKeyRepository);
        bob = new P2pServiceImpl(getNetNetworkConfigs(Config.Role.Bob), Config.bobKeyRepository);
        CountDownLatch serversReadyLatch = new CountDownLatch(2);
        alice.initializeServer((res, error) -> {
            if (res != null)
                log.error("Completed: {}", res.toString());
        }).whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });
        bob.initializeServer((res, error) -> {
            if (res != null)
                log.error(res.toString());
        }).whenComplete((result, throwable) -> {
            assertNotNull(result);
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(2, TimeUnit.MINUTES);
        assertTrue(serversReady);
    }

    @Test
    public void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        initializeServer();
        String msg = "hello";
        CountDownLatch receivedLatch = new CountDownLatch(1);
        bob.addMessageListener((message, connection) -> {
            log.error("message " + message);
            assertTrue(message instanceof MockMessage);
            assertEquals(((MockMessage) message).getMsg(), msg);
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(3);

        Address peerAddress = bob.getAddress(NetworkType.CLEAR).get();
        alice.confidentialSend(new MockMessage(msg), peerAddress, Config.keyPairBob.getPublic(), Config.keyPairAlice)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        peerAddress = bob.getAddress(NetworkType.TOR).get();
        alice.confidentialSend(new MockMessage(msg), peerAddress, Config.keyPairBob.getPublic(), Config.keyPairAlice)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        peerAddress = bob.getAddress(NetworkType.I2P).get();
        alice.confidentialSend(new MockMessage(msg), peerAddress, Config.keyPairBob.getPublic(), Config.keyPairAlice)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                    } else {
                        fail();
                    }
                });

        boolean sent = sentLatch.await(2, TimeUnit.MINUTES);
        assertTrue(sent);

        boolean received = receivedLatch.await(2, TimeUnit.MINUTES);
        assertTrue(received);
    }
}
