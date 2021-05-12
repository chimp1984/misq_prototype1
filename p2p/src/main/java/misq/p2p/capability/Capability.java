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

package misq.p2p.capability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.p2p.NetworkType;
import misq.p2p.node.Address;

import java.io.Serializable;
import java.util.Set;

@Getter
@EqualsAndHashCode
public class Capability implements Serializable {
    private final Address address;
    private final Set<NetworkType> supportedNetworkTypes;

    public Capability(Address address, Set<NetworkType> supportedNetworkTypes) {
        this.address = address;
        this.supportedNetworkTypes = supportedNetworkTypes;
    }

    @Override
    public String toString() {
        return "Capability{" +
                "\n     address=" + address +
                ",\n     supportedNetworkTypes=" + supportedNetworkTypes +
                "\n}";
    }
}
