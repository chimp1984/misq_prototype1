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
import misq.p2p.node.Connection;
import misq.p2p.node.Message;
import misq.p2p.node.MessageListener;

import java.util.function.Function;

@Slf4j
public class InventoryResponseHandler implements MessageListener, Disposable {
    private final Connection connection;
    private final Function<DataFilter, Inventory> inventoryProvider;
    private final Runnable completeHandler;

    public InventoryResponseHandler(Connection connection,
                                    Function<DataFilter, Inventory> inventoryProvider,
                                    Runnable completeHandler) {
        this.connection = connection;
        this.inventoryProvider = inventoryProvider;
        this.completeHandler = completeHandler;

        connection.addMessageListener(this);
    }

    public void dispose() {
        connection.removeMessageListener(this);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof InventoryRequest) {
            InventoryRequest request = (InventoryRequest) message;
            Inventory inventory = inventoryProvider.apply(request.getDataFilter());
            connection.send(new InventoryResponse(inventory));
            connection.removeMessageListener(this);
            completeHandler.run();
        }
    }

}
