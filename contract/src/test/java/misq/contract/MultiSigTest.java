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
import misq.contract.multiSig.MultiSigFactory;
import misq.contract.multiSig.MultiSigProtocol;
import misq.contract.multiSig.alice.AliceMultiSigProtocol;
import misq.contract.multiSig.bob.BobMultiSigProtocol;
import org.junit.Before;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
public abstract class MultiSigTest {
    private Network network;

    @Before
    public void setup() {
        // We share a network mock to call MessageListeners when sending a msg (e.g. alice send a msg and
        // bob receives the event)
        network = new Network();
    }

    protected abstract Chain getChain();

    protected abstract Wallet getAliceWallet();

    protected abstract Wallet getBobWallet();

    protected void run() {
        MultiSigFactory aliceFactory = new MultiSigFactory(getAliceWallet(), getChain());
        MultiSigFactory bobFactory = new MultiSigFactory(getBobWallet(), getChain());
        Execution alice = Execution.from(aliceFactory, Role.Alice, network);
        Execution bob = Execution.from(bobFactory, Role.Bob, network);

        CountDownLatch completedLatch = new CountDownLatch(2);
        alice.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.DEPOSIT_TX_CONFIRMED) {
                    // Simulate user action
                    new Timer("Simulate Alice user action").schedule(new TimerTask() {
                        public void run() {
                            ((AliceMultiSigProtocol) alice.getProtocol()).onFundsSent();
                        }
                    }, 400);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_VISIBLE_IN_MEM_POOL) {
                    completedLatch.countDown();
                }
            }
        });
        bob.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.FUNDS_SENT_MSG_RECEIVED) {
                    // Simulate user action
                    new Timer(" Simulate Bob user action").schedule(new TimerTask() {
                        public void run() {
                            ((BobMultiSigProtocol) bob.getProtocol()).onFundsReceived();
                        }
                    }, 400);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_BROADCAST_MSG_SENT) {
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
