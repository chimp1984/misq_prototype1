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
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class BaseTest {
    public enum Role {
        Alice,
        Bob,
        Carol
    }

    protected P2pService alice;
    protected P2pService bob;

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
                serverPort,
                List.of(Address.localHost(1000), Address.localHost(2000)));
        return List.of(clearNet);
    }

    @Before
    public void setup() {
        alice = new P2pService(getNetworkConfig(Role.Alice));
        bob = new P2pService(getNetworkConfig(Role.Bob));
    }

    @After
    public void shutdown() {
        alice.shutdown();
        bob.shutdown();
    }

    public void testBootstrap(int serversReadyLatchCount) throws InterruptedException {
        CountDownLatch serversReadyLatch = new CountDownLatch(serversReadyLatchCount);
        alice.bootstrap(serverInfo -> {
            serversReadyLatch.countDown();
        });
        bob.bootstrap(serverInfo -> {
            serversReadyLatch.countDown();
        });

        boolean serversReady = serversReadyLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(serversReady);
    }

    public void testConfidentialSend(NetworkType networkType) throws InterruptedException {
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
        alice.confidentialSend(new MockMessage(msg),
                peerAddress,
                connection -> {
                    sentLatch.countDown();
                });
        boolean sent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(sent);

        boolean received = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(received);
    }
}
