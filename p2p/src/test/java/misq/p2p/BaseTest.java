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
import misq.p2p.data.storage.Storage;

import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Slf4j
public abstract class BaseTest {

    protected final Storage storage = new Storage();
    protected P2pNode alice, bob, carol;

    protected abstract int getTimeout();

    protected abstract Set<NetworkType> getNetworkTypes();

    protected abstract NetworkConfig getNetworkConfig(Config.Role role);

    protected abstract Address getPeerAddress(Config.Role role);

    protected void testBootstrapSolo(int count) throws InterruptedException {
        alice = new P2pNode(getNetworkConfig(Config.Role.Alice), getNetworkTypes(), storage, Config.aliceKeyRepository);
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
                getNetworkTypes(), getNetworkTypes());
    }

    protected void testInitializeServer(int serversReadyLatchCount,
                                        NetworkConfig networkConfigAlice,
                                        NetworkConfig networkConfigBob,
                                        Set<NetworkType> supportedNetworkTypesAlice,
                                        Set<NetworkType> supportedNetworkTypesBob) throws InterruptedException {
        alice = new P2pNode(networkConfigAlice, supportedNetworkTypesAlice, storage, Config.aliceKeyRepository);
        bob = new P2pNode(networkConfigBob, supportedNetworkTypesBob, storage, Config.bobKeyRepository);
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
        alice.confidentialSend(new MockMessage(msg), peerAddress, Config.keyPairBob.getPublic(), Config.keyPairAlice)
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
