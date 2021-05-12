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

package misq.finance.swap.contract.bsqBond;

import misq.finance.contract.*;
import misq.p2p.P2pService;
import misq.p2p.endpoint.MessageListener;
/**
 * Mock protocol for simulating a BSQ bond based protocol
 * <p>
 * 1. Maker commits bond
 * 2. Maker sends commitment to Taker
 * 4. After Maker has received Takers commitment she sends her funds
 * 6. After Maker has received Takers funds she has completed.
 */

/**
 * Taker awaits Maker commitment
 * 3. After Taker has received Maker commitment he sends his commitment
 * 5. After Taker has received Maker funds he sends his funds. He has completed now.
 */
public abstract class BsqBondProtocol extends TwoPartyProtocol implements MessageListener {

    public enum State implements Protocol.State {
        START,
        COMMITMENT_SENT,
        COMMITMENT_RECEIVED,
        FUNDS_SENT,
        FUNDS_RECEIVED, // Completed
    }

    protected final AssetTransfer transport;
    protected final BsqBond security;

    public BsqBondProtocol(TwoPartyContract contract, P2pService p2pService, AssetTransfer transfer, SecurityProvider securityProvider) {
        super(contract, p2pService);
        this.transport = transfer;
        this.security = (BsqBond) securityProvider;
    }
}
