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

package misq.contract.multiSig.bob;


import lombok.extern.slf4j.Slf4j;
import misq.contract.Contract;
import misq.contract.Network;
import misq.contract.SecurityProvider;
import misq.contract.Transfer;
import misq.contract.multiSig.MultiSig;
import misq.contract.multiSig.MultiSigProtocol;
import misq.contract.multiSig.alice.FundsSentMessage;
import misq.contract.multiSig.alice.TxInputsMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class BobMultiSigProtocol extends MultiSigProtocol implements MultiSig.Listener {
    public BobMultiSigProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        super(contract, network, transfer, securityProvider);
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof TxInputsMessage) {
            TxInputsMessage txInputsMessage = (TxInputsMessage) message;
            multiSig.verifyTxInputsMessage(txInputsMessage)
                    .whenComplete((txInput, t) -> setState(State.TX_INPUTS_RECEIVED))
                    .thenCompose(txInput -> multiSig.broadcastDepositTx(txInput))
                    .whenComplete((depositTx, t) -> setState(State.DEPOSIT_TX_BROADCAST))
                    .thenCompose(depositTx -> network.send(new DepositTxBroadcastMessage(depositTx), contract.getPeer()))
                    .whenComplete((success, t) -> setState(State.DEPOSIT_TX_BROADCAST_MSG_SENT));
        } else if (message instanceof FundsSentMessage) {
            FundsSentMessage fundsSentMessage = (FundsSentMessage) message;
            multiSig.verifyFundsSentMessage(fundsSentMessage)
                    .whenComplete((signature, t) -> {
                        multiSig.setPayoutSignature(signature);
                        setState(State.FUNDS_SENT_MSG_RECEIVED);
                    });

        }
    }

    @Override
    public void onDepositTxConfirmed() {
        setState(State.DEPOSIT_TX_CONFIRMED);
    }

    public CompletableFuture<Boolean> start() {
        network.addListener(this);
        multiSig.addListener(this);
        setState(State.START);
        return CompletableFuture.completedFuture(true);
    }

    // Called by user or by altcoin explorer lookup
    public void onFundsReceived() {
        setState(State.FUNDS_RECEIVED);
        multiSig.broadcastPayoutTx()
                .whenComplete((payoutTx, t) -> setState(State.PAYOUT_TX_BROADCAST))
                .thenCompose(payoutTx -> network.send(new PayoutTxBroadcastMessage(payoutTx), contract.getPeer()))
                .whenComplete((isValid, t) -> setState(State.PAYOUT_TX_BROADCAST_MSG_SENT));
    }
}
