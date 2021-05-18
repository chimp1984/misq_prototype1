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
import java.util.Set;

@Slf4j
public class ClearNetIntegrationTest extends BaseTest {
    @Override
    protected Set<NetworkType> getNetworkTypes() {
        return Sets.newHashSet(NetworkType.CLEAR);
    }

    @Override
    protected int getTimeout() {
        return 10;
    }

    @Override
    protected NetworkConfig getNetworkConfig(Config.Role role) {
        return Config.getClearNetNetworkConfig(role);
    }

    @Override
    protected Address getPeerAddress(Config.Role role) {
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
