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
/**
 * Mock protocol for simulating the a basic 2of2 Multisig protocol (MAD).
 * Alice is BTC buyer and taker, Bob is seller and maker. There might be differences for buyer/seller roles
 * which would lead to 4 protocol variations, but we keep it simple and consider there are only 2 roles.
 * <p>
 * 1. Alice sends tx inputs
 * 3. Alice receives signed 2of MS tx from Bob, signs it and broadcasts it. Wait for confirmation
 * 4. After Tx is confirmed she sends funds to Bob and sends Bob a message including her signature for the payout tx.
 * 6. After Alice has received Bobs message and sees the payout tx in the network she has completed.
 */

/**
 * Bob awaits Alice commitment
 * 2. Bob receives tx inputs of alice and creates 2of MS tx, signs it and send it back to Alice.
 * Wait for confirmation once Alice has broadcast tx
 * 5. After Bob has received Alice message, checks if he has received the funds and if so he signs the payout tx and
 * broadcasts it and sends Alice a message that the payout tx is broadcast.
 */
public abstract class MultiSigProtocol extends Protocol implements Network.Listener {
    public enum State implements Protocol.State {
        START,
        TX_INPUTS_SENT,
        TX_INPUTS_RECEIVED,
        DEPOSIT_TX_BROADCAST,
        DEPOSIT_TX_BROADCAST_MSG_SENT,
        DEPOSIT_TX_BROADCAST_MSG_RECEIVED,
        DEPOSIT_TX_CONFIRMED,
        FUNDS_SENT,
        FUNDS_SENT_MSG_SENT,
        FUNDS_SENT_MSG_RECEIVED,
        FUNDS_RECEIVED,
        PAYOUT_TX_BROADCAST,
        PAYOUT_TX_BROADCAST_MSG_SENT, // Bob completed
        PAYOUT_TX_BROADCAST_MSG_RECEIVED,
        PAYOUT_TX_VISIBLE_IN_MEM_POOL // Alice completed
    }

    protected final AssetTransfer assetTransfer;
    protected final MultiSig multiSig;

    public MultiSigProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        super(contract, network);
        this.assetTransfer = (AssetTransfer) transfer;

        this.multiSig = (MultiSig) securityProvider;
    }
}
