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

package misq.p2p.data;

import misq.common.util.MapUtils;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.InventoryRequestHandler;
import misq.p2p.data.inventory.InventoryResponseHandler;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.data.router.Router;
import misq.p2p.data.router.gossip.BroadcastResult;
import misq.p2p.data.storage.MapKey;
import misq.p2p.data.storage.Storage;
import misq.p2p.guard.Guard;
import misq.p2p.node.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Responsibility:
 * -
 */
public class DataService implements MessageListener, ConnectionListener {
    private static final long BROADCAST_TIMEOUT = 90;

    private final Guard guard;
    private final Router router;
    private final Storage storage;
    private final Set<DataListener> dataListeners = new CopyOnWriteArraySet<>();
    private final Map<String, InventoryResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, InventoryRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public DataService(Guard guard, Storage storage) {
        this.guard = guard;
        this.storage = storage;

        router = new Router(guard);

        router.addMessageListener(this);
        guard.addConnectionListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof AddDataRequest) {
            AddDataRequest addDataRequest = (AddDataRequest) message;
            if (canAdd(addDataRequest)) {
                Message previousItem = storage.add(addDataRequest.getMessage());
                if (previousItem == null) {
                    dataListeners.forEach(listener -> listener.onDataAdded(message));
                }
            }
        } else if (message instanceof RemoveDataRequest) {
            RemoveDataRequest removeDataRequest = (RemoveDataRequest) message;
            if (canRemove(removeDataRequest)) {
                Message removedItem = storage.remove(removeDataRequest.getMapKey());
                if (removedItem != null) {
                    dataListeners.forEach(listener -> listener.onDataRemoved(message));
                }
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onInboundConnection(InboundConnection connection) {
        addResponseHandler(connection);
    }

    @Override
    public void onOutboundConnection(OutboundConnection connection, Address peerAddress) {
        addResponseHandler(connection);
    }

    @Override
    public void onDisconnect(Connection connection) {
        MapUtils.disposeAndRemove(connection.getUid(), responseHandlerMap);
        MapUtils.disposeAndRemove(connection.getUid(), requestHandlerMap);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<BroadcastResult> requestAddData(Message message) {
        AddDataRequest addDataRequest = new AddDataRequest(message);
        storage.add(addDataRequest);
        return router.broadcast(addDataRequest);
    }

    public CompletableFuture<BroadcastResult> requestRemoveData(Message message) {
        RemoveDataRequest removeDataRequest = new RemoveDataRequest(new MapKey(message));
        storage.remove(removeDataRequest.getMapKey());
        return router.broadcast(removeDataRequest);
    }

    public CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter) {
        return requestInventory(dataFilter, router.getPeerAddressesForInventoryRequest())
                .whenComplete((requestInventoryResult, throwable) -> {
                    if (requestInventoryResult != null) {
                        storage.add(requestInventoryResult.getInventory())
                                .handle((inventory, error) -> {
                                    if (inventory != null) {
                                        return requestInventoryResult;
                                    } else {
                                        return CompletableFuture.failedFuture(error);
                                    }
                                });
                    }
                });
    }

    public void addDataListener(DataListener listener) {
        dataListeners.add(listener);
    }

    public void removeDataListener(DataListener listener) {
        dataListeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter, Address address) {
        long ts = System.currentTimeMillis();
        CompletableFuture<RequestInventoryResult> future = new CompletableFuture<>();
        future.orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        guard.getConnection(address)
                .thenCompose(connection -> {
                    InventoryRequestHandler requestHandler = new InventoryRequestHandler(connection);
                    requestHandlerMap.put(connection.getUid(), requestHandler);
                    return requestHandler.request(dataFilter);
                })
                .whenComplete((inventory, throwable) -> {
                    if (inventory != null) {
                        future.complete(new RequestInventoryResult(inventory, System.currentTimeMillis() - ts));
                    } else {
                        future.completeExceptionally(throwable);
                    }
                });
        return future;
    }


    private void addResponseHandler(Connection connection) {
        InventoryResponseHandler responseHandler = new InventoryResponseHandler(connection,
                storage::getInventory,
                () -> responseHandlerMap.remove(connection.getUid()));
        responseHandlerMap.put(connection.getUid(), responseHandler);
    }

    private boolean canAdd(AddDataRequest message) {
        return true;
    }

    private boolean canRemove(RemoveDataRequest message) {
        return true;
    }

    public void shutdown() {
        dataListeners.clear();

        router.shutdown();
        storage.shutdown();

        MapUtils.disposeAndRemoveAll(requestHandlerMap);
        MapUtils.disposeAndRemoveAll(requestHandlerMap);
    }
}
