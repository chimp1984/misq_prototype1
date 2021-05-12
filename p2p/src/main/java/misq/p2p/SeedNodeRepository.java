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


import misq.p2p.endpoint.Address;

import java.util.Arrays;
import java.util.List;

public class SeedNodeRepository {

    public List<Address> getNodes(NetworkType networkType) {
        switch (networkType) {
            case TOR:
                return Arrays.asList(Address.localHost(1000), Address.localHost(1001));//todo
            case I2P:
                return Arrays.asList(Address.localHost(1000), Address.localHost(1001)); //todo
            default:
            case CLEAR:
                return Arrays.asList(Address.localHost(1000), Address.localHost(1001));
        }

    }
}
