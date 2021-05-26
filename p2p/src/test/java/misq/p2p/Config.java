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
import misq.common.security.KeyGeneration;
import misq.common.util.OsUtils;

import java.security.GeneralSecurityException;
import java.security.KeyPair;

@Slf4j
public abstract class Config {
    protected static KeyPair keyPairAlice1, keyPairBob1, keyPairAlice2, keyPairBob2;

    static {
        try {
            keyPairAlice1 = KeyGeneration.generateKeyPair();
            keyPairBob1 = KeyGeneration.generateKeyPair();
            keyPairAlice2 = KeyGeneration.generateKeyPair();
            keyPairBob2 = KeyGeneration.generateKeyPair();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    protected enum Role {
        Alice,
        Bob,
        Carol
    }

    final static KeyPairRepository aliceKeyPairSupplier1 = new KeyPairRepository(keyPairAlice1);
    final static KeyPairRepository aliceKeyPairSupplier2 = new KeyPairRepository(keyPairAlice2);
    final static KeyPairRepository bobKeyPairSupplier1 = new KeyPairRepository(keyPairBob1);
    final static KeyPairRepository bobKeyPairSupplier2 = new KeyPairRepository(keyPairBob2);

    static NetworkConfig getI2pNetworkConfig(Role role) {
        // Session in I2P need to be unique even if we use 2 diff SAM instances. So we add role name to default server id.
        return getI2pNetworkConfig(role, "default" + role);
    }

    static NetworkConfig getI2pNetworkConfig(Role role, String id) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NodeId nodeId = new NodeId(id, -1, Sets.newHashSet(NetworkType.I2P));
        return new NetworkConfig(baseDirName, nodeId, NetworkType.I2P);
    }

    static NetworkConfig getTorNetworkConfig(Role role) {
        return getTorNetworkConfig(role, "default", 9999);
    }

    static NetworkConfig getTorNetworkConfig(Role role, String id, int serverPort) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NodeId nodeId = new NodeId(id, serverPort, Sets.newHashSet(NetworkType.TOR));
        return new NetworkConfig(baseDirName, nodeId, NetworkType.TOR);
    }

    static NetworkConfig getClearNetNetworkConfig(Role role) {
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
        return getClearNetNetworkConfig(role, "default", serverPort);
    }

    static NetworkConfig getClearNetNetworkConfig(Role role, String id, int serverPort) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        NodeId nodeId = new NodeId(id, serverPort, Sets.newHashSet(NetworkType.CLEAR));
        return new NetworkConfig(baseDirName, nodeId, NetworkType.CLEAR);
    }
}
