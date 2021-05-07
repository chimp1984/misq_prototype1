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

package misq.contract.multiSig;

import misq.contract.*;
import misq.contract.multiSig.alice.AliceMultiSigProtocol;
import misq.contract.multiSig.bob.BobMultiSigProtocol;

public class MultiSigFactory implements Factory {
    private final Wallet wallet;
    private final Chain chain;

    public MultiSigFactory(Wallet wallet, Chain chain) {
        this.wallet = wallet;
        this.chain = chain;
    }

    @Override
    public Transfer getTransport() {
        return new AssetTransfer();
    }

    @Override
    public SecurityProvider getSecurity() {
        return new MultiSig(wallet, chain);
    }

    @Override
    public Protocol getProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        return new AliceMultiSigProtocol(contract, network, transfer, securityProvider);
    }

    @Override
    public Protocol getBobProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        return new BobMultiSigProtocol(contract, network, transfer, securityProvider);
    }
}
