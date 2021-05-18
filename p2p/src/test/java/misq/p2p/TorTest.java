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
import java.util.Optional;
import java.util.Set;

@Slf4j
public class TorTest extends BaseTest {
    protected int getTimeout() {
        return 180;
    }

    @Override
    protected Set<NetworkType> getNetworkTypes() {
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
        Optional<Address> optionalAddress = p2pNode.getAddress();
        if (optionalAddress.isPresent()) {
            return optionalAddress.get();
        } else {
            return new Address(persisted, serverPort);
        }
    }

    //  @Test
    public void testInitializeServer() throws InterruptedException {
        try {
            super.testInitializeServer(1);
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }

    @Test
    public void testConfidentialSend() throws InterruptedException, GeneralSecurityException {
        try {
            super.testConfidentialSend();
        } finally {
            alice.shutdown();
            bob.shutdown();
        }
    }
}
