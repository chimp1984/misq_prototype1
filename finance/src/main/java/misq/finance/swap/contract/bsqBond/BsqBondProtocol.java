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
import misq.p2p.node.MessageListener;

/**
 * Mock protocol for simulating a BSQ bond based protocol.
 * <ol>
 *   <li value="1">Maker commits bond.
 *   <li value="2">Maker sends commitment to Taker.
 *   <li value="4">After Maker has received Taker's commitment she sends her funds.
 *   <li value="6">After Maker has received Taker's funds she has completed.
 * </ol>
 * <p>
 * Taker awaits Maker commitment.
 * <ol>
 *   <li value="3">After Taker has received Maker's commitment he sends his commitment.
 *   <li value="5">After Taker has received Maker's funds he sends his funds. He has completed now.
 * </ol>
 */
public abstract class BsqBondProtocol extends TwoPartyProtocol implements MessageListener {

    public enum State implements Protocol.State {
        START,
        COMMITMENT_SENT,
        COMMITMENT_RECEIVED,
        FUNDS_SENT,
        FUNDS_RECEIVED // Completed
    }

    protected final AssetTransfer transport;
    protected final BsqBond security;

    public BsqBondProtocol(TwoPartyContract contract, P2pService p2pService, AssetTransfer transfer, SecurityProvider securityProvider) {
        super(contract, p2pService);
        this.transport = transfer;
        this.security = (BsqBond) securityProvider;
    }
}
