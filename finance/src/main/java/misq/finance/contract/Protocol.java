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

package misq.finance.contract;

import lombok.Getter;
import misq.p2p.P2pService;
import misq.p2p.node.MessageListener;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines the protocol for executing a contract.
 */
public abstract class Protocol implements MessageListener {
    public interface Listener {
        void onStateChange(State state);
    }

    public interface State {
    }

    @Getter
    protected final Contract contract;
    protected final P2pService p2pService;
    protected final Set<Listener> listeners = ConcurrentHashMap.newKeySet();

    public Protocol(Contract contract, P2pService p2pService) {
        this.contract = contract;
        this.p2pService = p2pService;
    }

    public abstract CompletableFuture<Boolean> start();

    public Protocol addListener(Listener listener) {
        listeners.add(listener);
        return this;
    }

    protected void setState(State state) {
        listeners.forEach(e -> e.onStateChange(state));
    }
}
