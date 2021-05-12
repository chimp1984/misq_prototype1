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


import lombok.extern.slf4j.Slf4j;
import misq.MockP2pService;
import misq.finance.Asset;
import misq.finance.ContractMaker;
import misq.finance.ProtocolType;
import misq.finance.TransferType;
import misq.finance.contract.ProtocolExecutor;
import misq.finance.contract.TwoPartyContract;
import misq.finance.swap.SwapProtocolType;
import misq.finance.swap.contract.bsqBond.maker.MakerBsqBondProtocol;
import misq.finance.swap.contract.bsqBond.taker.TakerBsqBondProtocol;
import misq.finance.swap.offer.SwapOffer;
import misq.p2p.P2pService;
import misq.p2p.endpoint.Address;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class BsqBondTest {

    private P2pService p2pService;

    @Before
    public void setup() {
        // We share a network mock to call MessageListeners when sending a msg (e.g. alice send a msg and
        // bob receives the event)
        p2pService = new MockP2pService();
    }

    @Test
    public void testBsqBond() {

        // create offer
        Address makerAddress = new Address("makerAddress");
        Asset askAsset = new Asset("USD", true, 100, List.of(TransferType.ZELLE));
        Asset bidAsset = new Asset("EUR", false, 90, List.of(TransferType.REVOLUT, TransferType.SEPA));
        SwapOffer offer = new SwapOffer(List.of(SwapProtocolType.BSQ_BOND, SwapProtocolType.REPUTATION),
                makerAddress, bidAsset, askAsset, Optional.empty());

        // taker takes offer and selects first ProtocolType
        ProtocolType selectedProtocolType = offer.getProtocolTypes().get(0);
        TwoPartyContract takerTrade = ContractMaker.createTakerTrade(offer, selectedProtocolType);
        TakerBsqBondProtocol takerBsqBondProtocol = new TakerBsqBondProtocol(takerTrade, p2pService);
        ProtocolExecutor takerSwapTradeProtocolExecutor = new ProtocolExecutor(takerBsqBondProtocol);

        // simulated take offer protocol: Taker sends to maker the selectedProtocolType
        Address takerAddress = new Address("takerAddress");
        TwoPartyContract makerTrade = ContractMaker.createMakerTrade(takerAddress, selectedProtocolType);
        MakerBsqBondProtocol makerBsqBondProtocol = new MakerBsqBondProtocol(makerTrade, p2pService);
        ProtocolExecutor makerSwapTradeProtocolExecutor = new ProtocolExecutor(makerBsqBondProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State) {
                var completedState = (BsqBondProtocol.State) state;
                if (completedState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });
        takerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State) {
                var completedState = (BsqBondProtocol.State) state;
                if (completedState == BsqBondProtocol.State.FUNDS_RECEIVED) {
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
