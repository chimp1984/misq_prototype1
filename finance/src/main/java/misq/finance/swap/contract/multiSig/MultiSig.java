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

package misq.finance.swap.contract.multiSig;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import misq.chain.Chain;
import misq.chain.Wallet;
import misq.finance.contract.SecurityProvider;
import misq.finance.swap.contract.multiSig.maker.FundsSentMessage;
import misq.finance.swap.contract.multiSig.maker.TxInputsMessage;
import misq.finance.swap.contract.multiSig.taker.DepositTxBroadcastMessage;
import misq.finance.swap.contract.multiSig.taker.PayoutTxBroadcastMessage;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MultiSig implements SecurityProvider, Chain.Listener {
    public interface Listener {
        void onDepositTxConfirmed();
    }

    private final Wallet wallet;
    private final Chain chain;
    @Setter
    private String depositTx;
    @Setter
    private String payoutSignature;

    protected final Set<MultiSig.Listener> listeners = ConcurrentHashMap.newKeySet();

    public MultiSig(Wallet wallet, Chain chain) {
        this.wallet = wallet;
        this.chain = chain;

        chain.addListener(this);
    }

    @Override
    public Type getType() {
        return Type.ESCROW;
    }

    @Override
    public void onTxConfirmed(String tx) {
        if (tx.equals(depositTx)) {
            listeners.forEach(Listener::onDepositTxConfirmed);
        }
    }

    public MultiSig addListener(MultiSig.Listener listener) {
        listeners.add(listener);
        return this;
    }

    public CompletableFuture<String> getTxInputs() {
        return wallet.getUtxos()
                .thenCompose(this::createPartialDepositTx);
    }

    private CompletableFuture<String> createPartialDepositTx(String utxos) {
        return CompletableFuture.completedFuture("partial deposit tx");
    }

    public CompletableFuture<String> broadcastDepositTx(String txInput) {
        return wallet.getUtxos()
                .thenCompose(utxos -> createDepositTx(txInput, utxos))
                .thenCompose(wallet::sign)
                .whenComplete((depositTx, t) -> this.depositTx = depositTx)
                .thenCompose(chain::broadcast);
    }

    private CompletableFuture<String> createDepositTx(String txInput, String utxos) {
        return CompletableFuture.completedFuture("depositTx");
    }

    public CompletableFuture<String> createPartialPayoutTx() {
        return CompletableFuture.completedFuture("payoutTx");
    }

    public CompletableFuture<String> createPayoutTx(String signature) {
        return CompletableFuture.completedFuture("payoutTx");
    }

    public CompletableFuture<String> getPayoutTxSignature(String payoutTx) {
        return wallet.sign(payoutTx);
    }

    public CompletableFuture<String> broadcastPayoutTx() {
        return createPayoutTx(payoutSignature)
                .thenCompose(wallet::sign)
                .thenCompose(chain::broadcast);
    }

    public CompletableFuture<Boolean> isPayoutTxInMemPool(String payoutTx) {
        return chain.isInMemPool(payoutTx);
    }


    public CompletableFuture<String> verifyTxInputsMessage(TxInputsMessage msg) {
        return CompletableFuture.completedFuture(msg.getTxInput());
    }

    public CompletableFuture<String> verifyDepositTxBroadcastMessage(DepositTxBroadcastMessage msg) {
        return CompletableFuture.completedFuture(msg.getTx());
    }

    public CompletableFuture<String> verifyPayoutTxBroadcastMessage(PayoutTxBroadcastMessage msg) {
        return CompletableFuture.completedFuture(msg.getTx());
    }

    public CompletableFuture<String> verifyFundsSentMessage(FundsSentMessage fundsSentMessage) {
        return CompletableFuture.completedFuture(fundsSentMessage.getSig());
    }
}
