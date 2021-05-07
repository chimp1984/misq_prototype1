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

package misq.contract;


import lombok.extern.slf4j.Slf4j;
import misq.contract.bsqBond.BsqBondFactory;
import misq.contract.bsqBond.BsqBondProtocol;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public class BsqBondTest {
    // public static void main(String[] args) {
    @Test
    public void testBsqBond() {
        BsqBondFactory factory = new BsqBondFactory();
        Network network = new Network();
        Execution alice = Execution.from(factory, Role.Alice, network);
        Execution bob = Execution.from(factory, Role.Bob, network);

        CountDownLatch completedLatch = new CountDownLatch(2);
        alice.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State) {
                var completedState = (BsqBondProtocol.State) state;
                if (completedState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });
        bob.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State) {
                var completedState = (BsqBondProtocol.State) state;
                if (completedState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });

        alice.start();
        bob.start();

        try {
            boolean completed = completedLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed);
        } catch (Throwable e) {
            fail(e.toString());
        }
    }
}
