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

package misq.p2p.data.inventory;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.Disposable;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.endpoint.Connection;
import misq.p2p.endpoint.Message;
import misq.p2p.endpoint.MessageListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InventoryRequestHandler implements MessageListener, Disposable {
    private static final long TIMEOUT_SEC = 90;

    private final Connection connection;
    private final CompletableFuture<Inventory> future = new CompletableFuture<>();

    public InventoryRequestHandler(Connection connection) {
        this.connection = connection;
    }

    public CompletableFuture<Inventory> request(DataFilter dataFilter) {
        future.orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
        connection.addMessageListener(this);
        connection.send(new InventoryRequest(dataFilter));
        return future;
    }

    public void dispose() {
        connection.removeMessageListener(this);
        future.cancel(true);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof InventoryResponse) {
            InventoryResponse inventoryResponse = (InventoryResponse) message;
            connection.removeMessageListener(this);
            future.complete(inventoryResponse.getInventory());
        }
    }
}
