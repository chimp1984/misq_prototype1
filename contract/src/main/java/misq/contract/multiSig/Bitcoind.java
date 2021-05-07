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

package misq.contract.multiSig;


import lombok.extern.slf4j.Slf4j;
import misq.contract.Chain;
import misq.contract.Wallet;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * In case our backend is a combined wallet and blockchain data provider we implement both interfaces
 */
@Slf4j
public class Bitcoind implements Wallet, Chain {

    protected final Set<Chain.Listener> listeners = new HashSet<>();

    public Bitcoind() {
    }

    // Wallet API
    @Override
    public CompletableFuture<String> getUtxos() {
        return CompletableFuture.completedFuture("utxos");
    }

    @Override
    public CompletableFuture<String> sign(String tx) {
        return CompletableFuture.completedFuture(tx);
    }

    // Chain API
    @Override
    public void addListener(Bitcoind.Listener listener) {
        listeners.add(listener);
    }

    @Override
    public CompletableFuture<String> broadcast(String tx) {
        // simulate confirmed state
        new Timer("Timer").schedule(new TimerTask() {
            public void run() {
                listeners.forEach(e -> e.onTxConfirmed(tx));
            }
        }, 900);
        return CompletableFuture.completedFuture(tx);
    }

    @Override
    public CompletableFuture<Boolean> isInMemPool(String tx) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Timer("Timer").schedule(new TimerTask() {
            public void run() {
                future.complete(true);
            }
        }, 200);
        return future;
    }
}
