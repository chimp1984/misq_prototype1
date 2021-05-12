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

package misq.finance.swap.contract.multiSig.taker;


import lombok.extern.slf4j.Slf4j;
import misq.finance.contract.AssetTransfer;
import misq.finance.contract.SecurityProvider;
import misq.finance.contract.TwoPartyContract;
import misq.finance.swap.contract.multiSig.MultiSig;
import misq.finance.swap.contract.multiSig.MultiSigProtocol;
import misq.finance.swap.contract.multiSig.maker.FundsSentMessage;
import misq.finance.swap.contract.multiSig.maker.TxInputsMessage;
import misq.p2p.P2pService;
import misq.p2p.endpoint.Connection;
import misq.p2p.endpoint.Message;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class TakerMultiSigProtocol extends MultiSigProtocol implements MultiSig.Listener {

    public TakerMultiSigProtocol(TwoPartyContract contract, P2pService p2pService, SecurityProvider securityProvider) {
        super(contract, p2pService, new AssetTransfer(), securityProvider);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof TxInputsMessage) {
            TxInputsMessage txInputsMessage = (TxInputsMessage) message;
            multiSig.verifyTxInputsMessage(txInputsMessage)
                    .whenComplete((txInput, t) -> setState(State.TX_INPUTS_RECEIVED))
                    .thenCompose(txInput -> multiSig.broadcastDepositTx(txInput))
                    .whenComplete((depositTx, t) -> setState(State.DEPOSIT_TX_BROADCAST))
                    .thenCompose(depositTx -> p2pService.confidentialSend(new DepositTxBroadcastMessage(depositTx), counterParty.getAddress()))
                    .whenComplete((connection1, t) -> setState(State.DEPOSIT_TX_BROADCAST_MSG_SENT));
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
        p2pService.addMessageListener(this);
        multiSig.addListener(this);
        setState(State.START);
        return CompletableFuture.completedFuture(true);
    }

    // Called by user or by altcoin explorer lookup
    public void onFundsReceived() {
        setState(State.FUNDS_RECEIVED);
        multiSig.broadcastPayoutTx()
                .whenComplete((payoutTx, t) -> setState(State.PAYOUT_TX_BROADCAST))
                .thenCompose(payoutTx -> p2pService.confidentialSend(new PayoutTxBroadcastMessage(payoutTx), counterParty.getAddress()))
                .whenComplete((isValid, t) -> setState(State.PAYOUT_TX_BROADCAST_MSG_SENT));
    }
}
