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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class Config {
    protected static KeyPair keyPairAlice1, keyPairBob1, keyPairAlice2, keyPairBob2;

    static {
        try {
            keyPairAlice1 = KeyPairGeneratorUtil.generateKeyPair();
            keyPairBob1 = KeyPairGeneratorUtil.generateKeyPair();
            keyPairAlice2 = KeyPairGeneratorUtil.generateKeyPair();
            keyPairBob2 = KeyPairGeneratorUtil.generateKeyPair();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    protected enum Role {
        Alice,
        Bob,
        Carol
    }


    final static Function<PublicKey, PrivateKey> alicePrivateKeySupplier1 = new Function<>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairAlice1.getPublic()));
            return keyPairAlice1.getPrivate();
        }
    };
    final static Function<PublicKey, PrivateKey> bobPrivateKeySupplier1 = new Function<>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairBob1.getPublic()));
            return keyPairBob1.getPrivate();
        }
    };
    final static Function<PublicKey, PrivateKey> alicePrivateKeySupplier2 = new Function<>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairAlice2.getPublic()));
            return keyPairAlice2.getPrivate();
        }
    };
    final static Function<PublicKey, PrivateKey> bobPrivateKeySupplier2 = new Function<>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairBob2.getPublic()));
            return keyPairBob2.getPrivate();
        }
    };

    static NetworkConfig getI2pNetworkConfig(Role role) {
        return getI2pNetworkConfig(role, "default");
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
