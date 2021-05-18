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
import misq.common.security.KeyPairGeneratorUtil;
import misq.common.util.OsUtils;
import misq.p2p.node.RawNode;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public abstract class Config {
    protected static KeyPair keyPairAlice, keyPairBob;

    static {
        try {
            keyPairAlice = KeyPairGeneratorUtil.generateKeyPair();
            keyPairBob = KeyPairGeneratorUtil.generateKeyPair();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    protected enum Role {
        Alice,
        Bob,
        Carol
    }


    final static Function<PublicKey, PrivateKey> aliceKeyRepository = new Function<>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairAlice.getPublic()));
            return keyPairAlice.getPrivate();
        }
    };
    final static Function<PublicKey, PrivateKey> bobKeyRepository = new Function<>() {
        @Override
        public PrivateKey apply(PublicKey publicKey) {
            checkArgument(publicKey.equals(keyPairBob.getPublic()));
            return keyPairBob.getPrivate();
        }
    };

    static NetworkConfig getI2pNetworkConfig(Role role) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        return new NetworkConfig(baseDirName, NetworkType.I2P, RawNode.DEFAULT_SERVER_ID + role.name(), -1);
    }

    static NetworkConfig getTorNetworkConfig(Role role) {
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
                NetworkType.TOR,
                RawNode.DEFAULT_SERVER_ID,
                serverPort);
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

        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        return new NetworkConfig(baseDirName,
                NetworkType.CLEAR,
                RawNode.DEFAULT_SERVER_ID,
                serverPort);
    }
}
