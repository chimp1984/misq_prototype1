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

import misq.common.security.DigestUtil;
import misq.p2p.data.storage.MapKey;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;

public class KeyPairRepository {
    private final Map<MapKey, KeyPair> keyPairsByPubKeyHash = new ConcurrentHashMap<>();

    public KeyPairRepository(KeyPair keyPair) {
        add(keyPair);
    }

    public void add(KeyPair keyPair) {
        MapKey key = new MapKey(DigestUtil.hash(keyPair.getPublic().getEncoded()));
        checkArgument(!keyPairsByPubKeyHash.containsKey(key));
        keyPairsByPubKeyHash.put(key, keyPair);
    }

    public Optional<KeyPair> findKeyPair(byte[] hashOfPublicKey) {
        MapKey key = new MapKey(hashOfPublicKey);
        if (keyPairsByPubKeyHash.containsKey(key)) {
            return Optional.of(keyPairsByPubKeyHash.get(key));
        } else {
            return Optional.empty();
        }
    }
}
