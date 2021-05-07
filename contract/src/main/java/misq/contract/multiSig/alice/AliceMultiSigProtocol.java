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

package misq.contract.multiSig.alice;


import lombok.extern.slf4j.Slf4j;
import misq.contract.Contract;
import misq.contract.Network;
import misq.contract.SecurityProvider;
import misq.contract.Transfer;
import misq.contract.multiSig.MultiSig;
import misq.contract.multiSig.MultiSigProtocol;
import misq.contract.multiSig.bob.DepositTxBroadcastMessage;
import misq.contract.multiSig.bob.PayoutTxBroadcastMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AliceMultiSigProtocol extends MultiSigProtocol implements MultiSig.Listener {
    public AliceMultiSigProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        super(contract, network, transfer, securityProvider);
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof DepositTxBroadcastMessage) {
            DepositTxBroadcastMessage depositTxBroadcastMessage = (DepositTxBroadcastMessage) message;
            multiSig.verifyDepositTxBroadcastMessage(depositTxBroadcastMessage)
                    .whenComplete((depositTx, t) -> {
                        multiSig.setDepositTx(depositTx);
                        setState(State.DEPOSIT_TX_BROADCAST_MSG_RECEIVED);
                    });
        } else if (message instanceof PayoutTxBroadcastMessage) {
            PayoutTxBroadcastMessage payoutTxBroadcastMessage = (PayoutTxBroadcastMessage) message;
            multiSig.verifyPayoutTxBroadcastMessage(payoutTxBroadcastMessage)
                    .whenComplete((payoutTx, t) -> setState(State.PAYOUT_TX_BROADCAST_MSG_RECEIVED))
                    .thenCompose(payoutTx -> multiSig.isPayoutTxInMemPool(payoutTx))
                    .whenComplete((isInMemPool, t) -> setState(State.PAYOUT_TX_VISIBLE_IN_MEM_POOL));
        }
    }

    @Override
    public void onDepositTxConfirmed() {
        setState(State.DEPOSIT_TX_CONFIRMED);
        // Client need to listen on that state change and instruct user to send funds or send
        // altcoin via altcoin wallet bridge if implemented
    }

    public CompletableFuture<Boolean> start() {
        network.addListener(this);
        multiSig.addListener(this);
        setState(State.START);
        multiSig.getTxInputs()
                .thenCompose(txInputs -> network.send(new TxInputsMessage(txInputs), contract.getPeer()))
                .whenComplete((success, t) -> setState(State.TX_INPUTS_SENT));
        return CompletableFuture.completedFuture(true);
    }

    // Called by user or by altcoin explorer lookup
    public void onFundsSent() {
        assetTransfer.sendFunds(contract)
                .whenComplete((isValid, t) -> setState(State.FUNDS_SENT))
                .thenCompose(e -> multiSig.createPartialPayoutTx())
                .thenCompose(partialPayoutTx -> multiSig.getPayoutTxSignature(partialPayoutTx))
                .thenCompose(sig -> network.send(new FundsSentMessage(sig), contract.getPeer()))
                .whenComplete((isValid, t) -> setState(State.FUNDS_SENT_MSG_SENT));
    }
}
