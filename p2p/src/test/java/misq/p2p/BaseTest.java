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
import misq.common.security.KeyPairGeneratorUtil;
import misq.common.util.OsUtils;
import misq.p2p.data.storage.Storage;
import misq.p2p.node.RawNode;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.*;

@Slf4j
public class BaseTest {
    private Set<NetworkType> mySupportedNetworks = Sets.newHashSet(NetworkType.CLEAR);
    private Storage storage = new Storage();

    protected enum Role {
        Alice,
        Bob,
        Carol
    }

    protected P2pNode alice;
    protected P2pNode bob;

    protected int getTimeout() {
        return 10;
    }

    protected NetworkConfig getNetworkConfig(Role role) {
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
                RawNode.DEFAULT_SERVER_ID,
                serverPort);
        return clearNet;
    }

    private static KeyPair keyPairAlice, keyPairBob;

    static {
        try {
            keyPairAlice = KeyPairGeneratorUtil.generateKeyPair();
            keyPairBob = KeyPairGeneratorUtil.generateKeyPair();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }


    Function<PublicKey, PrivateKey> aliceKeyRepository = new Function<PublicKey, PrivateKey>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairAlice.getPublic()));
            return keyPairAlice.getPrivate();
        }
    };
    Function<PublicKey, PrivateKey> bobKeyRepository = new Function<PublicKey, PrivateKey>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairBob.getPublic()));
            return keyPairBob.getPrivate();
        }
    };

    protected void testBootstrapSolo(int count) throws InterruptedException {
        alice = new P2pNode(getNetworkConfig(Role.Alice), mySupportedNetworks, storage, aliceKeyRepository);
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
        alice = new P2pNode(getNetworkConfig(Role.Alice), mySupportedNetworks, storage, aliceKeyRepository);
        bob = new P2pNode(getNetworkConfig(Role.Bob), mySupportedNetworks, storage, bobKeyRepository);
        CountDownLatch serversReadyLatch = new CountDownLatch(serversReadyLatchCount);
        alice.initializeServer().whenComplete((result, throwable) -> {
            serversReadyLatch.countDown();
        });
        bob.initializeServer().whenComplete((result, throwable) -> {
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    protected void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        testInitializeServer(2);
        NetworkConfig networkConfigBob = getNetworkConfig(Role.Bob);
        String msg = "hello";
        CountDownLatch receivedLatch = new CountDownLatch(1);
        bob.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            assertEquals(((MockMessage) message).getMsg(), msg);
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(1);

        Address peerAddress = Address.localHost(networkConfigBob.getServerPort());
        alice.confidentialSend(new MockMessage(msg), peerAddress, keyPairBob.getPublic(), keyPairAlice)
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
