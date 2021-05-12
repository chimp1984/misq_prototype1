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
import misq.p2p.node.Node;
import org.junit.Test;

@Slf4j
public class ClearNetTest extends BaseTest {
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
                Node.DEFAULT_SERVER_ID,
                serverPort);
        return clearNet;
    }

    //@Test
    public void testInitializeServer() throws InterruptedException {
        super.testInitializeServer(2);
        alice.shutdown();
        bob.shutdown();
    }

    //@Test
    public void testConfidentialSend() throws InterruptedException {
        super.testConfidentialSend();
        alice.shutdown();
        bob.shutdown();
    }

    @Test
    public void testPeerExchange() throws InterruptedException {
      /*  bootstrapSeedNode();
        shutDownSeed();

        bootstrapSeedNodeAndNode1();
        shutDownSeed();
        shutDownNode1();*/

      /*  bootstrapSeedNodeAndNode1AndNode2();
       shutDownSeed();
        shutDownNode1();
        shutDownNode2();*/

        bootstrapSeedNodeAndNode1AndNode2AndNode3();
        shutDownSeed();
        shutDownNode1();
        shutDownNode2();
        shutDownNode3();
    }
}
