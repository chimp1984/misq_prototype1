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

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@EqualsAndHashCode
public class NetworkPeer {
    private final Map<NetworkType, Address> addressByNetworkType = new ConcurrentHashMap<>();
    private final PublicKey publicKey;
    // The tag is used as key in the map in the keypair repository. For decrypting messages it is used to select the correct key.
    private final String tag;

    public NetworkPeer(Address address, PublicKey publicKey, String tag) {
        this(Set.of(address), publicKey, tag);
    }

    public NetworkPeer(Set<Address> addressList, PublicKey publicKey, String tag) {
        addressList.forEach(e -> addressByNetworkType.put(e.getNetworkType(), e));
        this.publicKey = publicKey;
        this.tag = tag;
    }

    public Address getAddress(NetworkType networkType) {
        return addressByNetworkType.get(networkType);
    }

    public Optional<Address> findAddress(NetworkType networkType) {
        return Optional.ofNullable(addressByNetworkType.get(networkType));
    }

    @Override
    public String toString() {
        return "NetworkPeer{" +
                "\n     addressByNetworkType=" + addressByNetworkType +
                ",\n     publicKey=" + publicKey +
                ",\n     tag='" + tag + '\'' +
                "\n}";
    }
}
