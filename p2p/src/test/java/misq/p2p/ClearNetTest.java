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
import misq.common.util.OsUtils;
import misq.p2p.node.RawNode;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.util.HashSet;

@Slf4j
public class ClearNetTest extends BaseTest {
    @Override
    protected HashSet<NetworkType> getNetworkTypes() {
        return Sets.newHashSet(NetworkType.CLEAR);
    }

    @Override
    protected int getTimeout() {
        return 10;
    }

    @Override
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
        return new NetworkConfig(baseDirName,
                NetworkType.CLEAR,
                RawNode.DEFAULT_SERVER_ID,
                serverPort);
    }

    @Override
    protected Address getPeerAddress(Role role) {
        return Address.localHost(getNetworkConfig(role).getServerPort());
    }

    //@Test
    public void testInitializeServer() throws InterruptedException {
        super.testInitializeServer(2);
        alice.shutdown();
        bob.shutdown();
    }

    @Test
    public void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        super.testConfidentialSend();
        alice.shutdown();
        bob.shutdown();
    }
}
