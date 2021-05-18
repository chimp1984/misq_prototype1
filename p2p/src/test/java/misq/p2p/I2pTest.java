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
import java.util.Optional;

@Slf4j
public class I2pTest extends BaseTest {
    @Override
    protected int getTimeout() {
        return 120;
    }

    @Override
    protected HashSet<NetworkType> getNetworkTypes() {
        return Sets.newHashSet(NetworkType.I2P);
    }

    @Override
    protected NetworkConfig getNetworkConfig(Role role) {
        String baseDirName = OsUtils.getUserDataDir().getAbsolutePath() + "/misq_test_" + role.name();
        return new NetworkConfig(baseDirName, NetworkType.I2P, RawNode.DEFAULT_SERVER_ID + role.name(), -1);
    }

    @Override
    protected Address getPeerAddress(Role role) {
        P2pNode p2pNode;
        String persisted = "undefined";
        switch (role) {
            case Alice:
                p2pNode = this.alice;
                persisted = "TAxJNls3m3moGJU4hS7KxewxVLs1emS2N6WmYegpJk9mX7VSYwY-1j2fvnGBJptM~3oZIJ-HChfmb1GtxOL14zkV~exl6ZRXpZnbD4i6oP2TZg5eD8H5vOu0q-NBC2xjxGKPXOGGUmDi1bzqzcumOYSzJI57OVlOLNo8bkGfiLJuFhVWQk-PquQvFmJcp5fAYfgNJCL0eE9YpCSXc2SepopD02rr-WLmkl3qKH1xzS8jlMSN0-PS35TzU1YSzSzFmjUP7llmNCAINu5z9fZiUMObMhpbrGJCR2-k~0g-ujVnZ-0PuBIncaOAYWPloCeeKt7NMo406Wtjxjmz4VARUcGbqoe11lqyzMKD33LKPVIa56XFJRjg~d7j3dmTo-kR0aarLWZlDKf6AjTisLGNPEmvSzY4Z91dHVBqNWy6PNtXpqeETss3YdHUnGKZlFOdxDrRJpTuk56itDuvwwEpWWP1iZhBEap4IPJaHCIGsEObf0TW4lVwUk2BwyI9Ue7SBQAEAAcAAA==";
                break;
            case Bob:
                p2pNode = this.bob;
                persisted = "QXOufFlBKxOJOpYqo5mXqOotCaUwkrfUphjX4Qz9IOmKjcmIFdNKBBTi5fmdJkKzsQozWu~fYA~5aoy42YCioEsa42YL2ewPbg6xgkwMIb~aPOTXnfJh1kSDItQBrVSv9rX3uYSvj-j-tT48cHkO4aBcCNm61-u8goBd8ANOqVaS-a1v5Jde~8dY-V-D3cDqFgHxP8D6YamN3shEJQP4BhEglfbdXf~hneja77uGFu20h-B5yBIOqelu6~wEIG2t85q~lyRfL~grC1y524HeWV4Pe8xtbXatcBsF5DSKGUjZXy0vXHrbHiasGTSJS9qpVMS9ue6NZ3dyAfIVv2qh0CltLTaqj~83YvuOGuOpOfU3ZTSP2yhHJo7kUe7rjiA7DGZ3D6-I0q1mbzvlnSoe4ftlcXqnzWglem-8zuxdlW-e38~0ac-hAxde8-4HhUHzE32~8xp~-r4CWNxQ4qWOexlXJH9s8pcdCelSnvQ7J8H2CDIEQ5deXkYBp6LTIVvyBQAEAAcAAA==";
                break;
            case Carol:
            default:
                p2pNode = this.carol;
                break;
        }
        Optional<Address> optionalAddress = p2pNode.getAddress();
        if (optionalAddress.isPresent()) {
            return optionalAddress.get();
        } else {
            return new Address(persisted, -1);
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
