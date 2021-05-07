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

package misq.contract.bsqBond;

import misq.contract.*;
import misq.contract.bsqBond.alice.AliceBsqBondProtocol;
import misq.contract.bsqBond.bob.BobBsqBondProtocol;

public class BsqBondFactory implements Factory {
    @Override
    public Transfer getTransport() {
        return new AssetTransfer();
    }

    @Override
    public SecurityProvider getSecurity() {
        return new BsqBond();
    }

    @Override
    public Protocol getProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        return new AliceBsqBondProtocol(contract, network, transfer, securityProvider);
    }

    @Override
    public Protocol getBobProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        return new BobBsqBondProtocol(contract, network, transfer, securityProvider);
    }
}
