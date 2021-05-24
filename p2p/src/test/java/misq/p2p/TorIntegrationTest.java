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
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class TorIntegrationTest extends BaseTest {
    protected int getTimeout() {
        return 180;
    }

    @Override
    protected Set<NetworkType> getMySupportedNetworks() {
        return Sets.newHashSet(NetworkType.TOR);
    }

    @Override
    protected NetworkConfig getNetworkConfig(Config.Role role) {
        return Config.getTorNetworkConfig(role);
    }

    @Override
    protected Address getPeerAddress(Config.Role role) {
        P2pNode p2pNode;
        int serverPort;
        String persisted = "undefined";
        switch (role) {
            case Alice:
                p2pNode = this.alice;
                serverPort = 1111;
                persisted = "v3vis457zpzqshbovnixgefaylj7dks3cfpodvucgc33w4hytwqxwyqd.onion";
                break;
            case Bob:
                p2pNode = this.bob;
                serverPort = 2222;
                persisted = "r4guo4fillnhk43c7dplrqefdpjcoprwh6d7gmwomvhix6rnpdvd3zyd.onion";
                break;
            case Carol:
            default:
                p2pNode = this.carol;
                serverPort = 3333;
                break;
        }
        Optional<Address> optionalAddress = p2pNode.findMyAddress();
        if (optionalAddress.isPresent()) {
            return optionalAddress.get();
        } else {
            return new Address(persisted, serverPort);
        }
    }

    //  @Test
    public void testInitializeServer() throws InterruptedException {
        try {
            super.testInitializeServer(2);
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }

    // @Test
    public void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        try {
            super.testConfidentialSend();
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }

    @Test
    public void testStartOfMultipleIds() throws InterruptedException {
        NetworkType networkType = NetworkType.TOR;
        Set<NetworkType> mySupportedNetworks = getMySupportedNetworks();
        startOfMultipleIds(networkType, mySupportedNetworks);
    }

    // @Test
    public void testSendMsgWithMultipleIds() throws InterruptedException, GeneralSecurityException {
        NetworkType networkType = NetworkType.TOR;
        Set<NetworkType> mySupportedNetworks = getMySupportedNetworks();
        sendMsgWithMultipleIds(networkType, mySupportedNetworks);
    }


    // First msg: 7905 ms, others 400-600ms
    // First msg: 10030 ms, others 500-700
    // Total: 16693 ms / 24742 ms /  19364 ms /  16836 ms
    // @Test
    public void repeatedSend() throws InterruptedException, GeneralSecurityException {
        long ts = System.currentTimeMillis();
        testInitializeServer(2);

        int numMsg = 10;
        Map<Integer, Long> tsMap = new HashMap<>();
        CountDownLatch receivedLatch = new CountDownLatch(numMsg);
        bob.addMessageListener((message, connection) -> {
            assertTrue(message instanceof MockMessage);
            // assertEquals(((MockMessage) message).getMsg(), msg);
            int key = Integer.parseInt(((MockMessage) message).getMsg());
            if (tsMap.containsKey(key)) {
                log.error("Sending msg {} took {}", key, System.currentTimeMillis() - tsMap.get(key));
            }
            receivedLatch.countDown();
        });
        CountDownLatch sentLatch = new CountDownLatch(numMsg);

        Address peerAddress = getPeerAddress(Config.Role.Bob);
        send(sentLatch, peerAddress, new AtomicInteger(0), tsMap);
        boolean sent = sentLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(sent);

        boolean received = receivedLatch.await(getTimeout(), TimeUnit.SECONDS);
        assertTrue(received);

        alice.shutdown();
        bob.shutdown();
        log.error("Took {} ms", System.currentTimeMillis() - ts);
    }

    private void send(CountDownLatch sentLatch, Address peerAddress, AtomicInteger i, Map<Integer, Long> tsMap) throws GeneralSecurityException {
        tsMap.put(i.get(), System.currentTimeMillis());
        log.error("Send msg {}", i.get());
        alice.confidentialSend(new MockMessage(String.valueOf(i.get())), peerAddress, Config.keyPairBob1.getPublic(), Config.keyPairAlice1)
                .whenComplete((connection, throwable) -> {
                    if (connection != null) {
                        sentLatch.countDown();
                        if (sentLatch.getCount() > 0) {
                            try {
                                i.incrementAndGet();
                                send(sentLatch, peerAddress, i, tsMap);
                            } catch (GeneralSecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        fail();
                    }
                });
    }
}
