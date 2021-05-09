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


import lombok.extern.slf4j.Slf4j;
import misq.MockP2pService;
import misq.chain.Chain;
import misq.chain.Wallet;
import misq.finance.Asset;
import misq.finance.ContractMaker;
import misq.finance.ProtocolType;
import misq.finance.TransferType;
import misq.finance.contract.ProtocolExecutor;
import misq.finance.contract.TwoPartyContract;
import misq.finance.swap.SwapProtocolType;
import misq.finance.swap.contract.multiSig.maker.MakerMultiSigProtocol;
import misq.finance.swap.contract.multiSig.taker.TakerMultiSigProtocol;
import misq.finance.swap.offer.SwapOffer;
import misq.p2p.P2pService;
import misq.p2p.node.Address;
import org.junit.Before;

import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public abstract class MultiSigTest {
    private P2pService p2pService;

    @Before
    public void setup() {
        // We share a network mock to call MessageListeners when sending a msg (e.g. alice send a msg and
        // bob receives the event)
        p2pService = new MockP2pService();
    }

    protected abstract Chain getChain();

    protected abstract Wallet getTakerWallet();

    protected abstract Wallet getMakerWallet();

    protected void run() {
        P2pService p2pService = new MockP2pService();
        // create offer
        Address makerAddress = new Address("makerAddress");
        Asset askAsset = new Asset("USD", true, 50000, List.of(TransferType.ZELLE));
        Asset bidAsset = new Asset("BTC", false, 1, List.of());
        SwapOffer offer = new SwapOffer(List.of(SwapProtocolType.MULTISIG),
                makerAddress, bidAsset, askAsset, Optional.empty());

        // taker takes offer and selects first ProtocolType
        ProtocolType selectedProtocolType = offer.getProtocolTypes().get(0);
        TwoPartyContract takerTrade = ContractMaker.createTakerTrade(offer, selectedProtocolType);
        MultiSig takerMultiSig = new MultiSig(getTakerWallet(), getChain());
        TakerMultiSigProtocol takerMultiSigProtocol = new TakerMultiSigProtocol(takerTrade, p2pService, takerMultiSig);
        ProtocolExecutor takerSwapTradeProtocolExecutor = new ProtocolExecutor(takerMultiSigProtocol);

        // simulated take offer protocol: Taker sends to maker the selectedProtocolType
        Address takerAddress = new Address("takerAddress");
        TwoPartyContract makerTrade = ContractMaker.createMakerTrade(takerAddress, selectedProtocolType);
        MultiSig makerMultiSig = new MultiSig(getMakerWallet(), getChain());
        MakerMultiSigProtocol makerMultiSigProtocol = new MakerMultiSigProtocol(makerTrade, p2pService, makerMultiSig);
        ProtocolExecutor makerSwapTradeProtocolExecutor = new ProtocolExecutor(makerMultiSigProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.DEPOSIT_TX_CONFIRMED) {
                    // Simulate user action
                    new Timer(" Simulate Bob user action").schedule(new TimerTask() {
                        public void run() {
                            ((MakerMultiSigProtocol) makerSwapTradeProtocolExecutor.getProtocol()).onFundsSent();
                        }
                    }, 40);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_VISIBLE_IN_MEM_POOL) {
                    completedLatch.countDown();
                }
            }
        });
        takerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.FUNDS_SENT_MSG_RECEIVED) {
                    // Simulate user action
                    new Timer("Simulate Alice user action").schedule(new TimerTask() {
                        public void run() {
                            ((TakerMultiSigProtocol) takerSwapTradeProtocolExecutor.getProtocol()).onFundsReceived();
                        }
                    }, 40);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_BROADCAST_MSG_SENT) {
                    completedLatch.countDown();
                }
            }
        });

        makerSwapTradeProtocolExecutor.start();
        takerSwapTradeProtocolExecutor.start();

        try {
            boolean completed = completedLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed);
        } catch (Throwable e) {
            fail(e.toString());
        }
    }
}
