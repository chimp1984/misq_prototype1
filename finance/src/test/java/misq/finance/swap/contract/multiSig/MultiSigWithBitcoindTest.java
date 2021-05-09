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
import misq.chain.Bitcoind;
import misq.chain.Chain;
import misq.chain.Wallet;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class MultiSigWithBitcoindTest extends MultiSigTest {

    private Chain chain;

    @Before
    public void setup() {
        super.setup();
        // A shared chain data provider is used to mock broadcasts and mocked block events
        // (e.g. bob broadcasts and both get the confirm event)
        chain = new Bitcoind();
    }

    protected Chain getChain() {
        return chain; // mock shared between both nodes
    }

    protected Wallet getTakerWallet() {
        return new Bitcoind();
    }

    protected Wallet getMakerWallet() {
        return new Bitcoind();
    }

    @Test
    public void testMultiSig() {
        super.run();
    }
}
