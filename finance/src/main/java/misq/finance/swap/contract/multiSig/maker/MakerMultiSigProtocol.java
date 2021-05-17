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

package misq.finance.swap.contract.multiSig.maker;


import lombok.extern.slf4j.Slf4j;
import misq.finance.contract.AssetTransfer;
import misq.finance.contract.SecurityProvider;
import misq.finance.contract.TwoPartyContract;
import misq.finance.swap.contract.multiSig.MultiSig;
import misq.finance.swap.contract.multiSig.MultiSigProtocol;
import misq.finance.swap.contract.multiSig.taker.DepositTxBroadcastMessage;
import misq.finance.swap.contract.multiSig.taker.PayoutTxBroadcastMessage;
import misq.p2p.P2pService;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class MakerMultiSigProtocol extends MultiSigProtocol implements MultiSig.Listener {
    public MakerMultiSigProtocol(TwoPartyContract contract, P2pService p2pService, SecurityProvider securityProvider) {
        super(contract, p2pService, new AssetTransfer(), securityProvider);
    }

    @Override
    public void onMessage(Message message, Connection connection) {
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
        p2pService.addMessageListener(this);
        multiSig.addListener(this);
        setState(State.START);
        multiSig.getTxInputs()
                .thenCompose(txInputs -> p2pService.confidentialSend(new TxInputsMessage(txInputs), counterParty.getAddress()))
                .whenComplete((success, t) -> setState(State.TX_INPUTS_SENT));
        return CompletableFuture.completedFuture(true);
    }

    // Called by user or by altcoin explorer lookup
    public void onFundsSent() {
        assetTransfer.sendFunds(contract)
                .whenComplete((isValid, t) -> setState(State.FUNDS_SENT))
                .thenCompose(e -> multiSig.createPartialPayoutTx())
                .thenCompose(partialPayoutTx -> multiSig.getPayoutTxSignature(partialPayoutTx))
                .thenCompose(sig -> p2pService.confidentialSend(new FundsSentMessage(sig), counterParty.getAddress()))
                .whenComplete((isValid, t) -> setState(State.FUNDS_SENT_MSG_SENT));
    }
}
