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

import java.util.List;

@Slf4j
public class TorTest extends BaseTest {
    protected int getTimeout() {
        return 180;
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

        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_TorTest_" + role.name();
        NetworkConfig tor = new NetworkConfig(baseDirName,
                NetworkType.TOR,
                Node.DEFAULT_SERVER_ID,
                serverPort);
        return List.of(tor);
    }

    // @Test
    public void testBootstrap() throws InterruptedException {
        super.testBootstrap(2);
    }

    @Test
    public void testConfidentialSend() throws InterruptedException {
        super.testBootstrap(2);
        super.testConfidentialSend(NetworkType.TOR);
    }
}
