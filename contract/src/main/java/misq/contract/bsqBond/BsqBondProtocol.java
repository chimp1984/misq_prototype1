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
/**
 * Mock protocol for simulating a BSQ bond based trade protocol
 * <p>
 * 1. Alice commits bond
 * 2. Alice sends commitment to Bob
 * 4. After Alice has received Bobs commitment she sends her funds
 * 6. After Alice has received Bobs funds she has completed.
 */

/**
 * Bob awaits Alice commitment
 * 3. After Bob has received Alice commitment he sends his commitment
 * 5. After Bob has received Alice funds he sends his funds. He has completed now.
 */
public abstract class BsqBondProtocol extends Protocol implements Network.Listener {
    public enum State implements Protocol.State {
        START,
        COMMITMENT_SENT,
        COMMITMENT_RECEIVED,
        FUNDS_SENT,
        FUNDS_RECEIVED, // Completed
    }

    protected final AssetTransfer transport;
    protected final BsqBond security;

    public BsqBondProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        super(contract, network);
        this.transport = (AssetTransfer) transfer;
        this.security = (BsqBond) securityProvider;
    }
}
