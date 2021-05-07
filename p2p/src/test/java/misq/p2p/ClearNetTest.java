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
import org.junit.Test;

import java.util.List;

@Slf4j
public class ClearNetTest extends BaseTest {
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

  /*  @Before
    public void setup() {
        alice = new P2pService(getNetworkConfig(Role.Alice));
        bob = new P2pService(getNetworkConfig(Role.Bob));
    }

    @After
    public void shutdown() {
        alice.shutdown();
        bob.shutdown();
    }*/

    // @Test
    public void testBootstrap() throws InterruptedException {
        super.testBootstrap(2);
    }

    //@Test
    public void testConfidentialSend() throws InterruptedException {
        super.testBootstrap(2);
        super.testConfidentialSend(NetworkType.CLEAR);
    }

    @Test
    public void testConfidentialSends() throws InterruptedException {
        super.testBootstrap(2);
        super.testRequestAddData(NetworkType.CLEAR);
    }
}
