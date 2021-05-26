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

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

public class KeyPairRepository {
    private final Map<String, KeyPair> keyPairsByPubKeyHash = new ConcurrentHashMap<>();

    public KeyPairRepository(KeyPair keyPair) {
        add(keyPair, "default");
    }

    public void add(KeyPair keyPair, String tag) {
        checkArgument(!keyPairsByPubKeyHash.containsKey(tag));
        keyPairsByPubKeyHash.put(tag, keyPair);
    }

    public Optional<KeyPair> findKeyPair(String tag) {
        if (keyPairsByPubKeyHash.containsKey(tag)) {
            return Optional.of(keyPairsByPubKeyHash.get(tag));
        } else {
            return Optional.empty();
        }
    }
}
