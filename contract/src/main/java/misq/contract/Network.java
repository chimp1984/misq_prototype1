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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Mock for simulating a p2p network
 */
@Slf4j
public class Network {
    public interface Listener {
        void onMessage(Object message);
    }

    public interface Message {
    }

    private Set<Listener> listeners = new HashSet<>();

    public CompletableFuture<Boolean> send(Message message, Peer peer) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            future.complete(true);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
            listeners.forEach(e -> e.onMessage(message));
        }).start();

        return future;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

}
