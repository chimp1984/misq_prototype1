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
import misq.p2p.Address;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.InventoryRequestHandler;
import misq.p2p.data.inventory.InventoryResponseHandler;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.data.storage.Storage;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.ConnectionListener;
import misq.p2p.node.MessageListener;
import misq.p2p.node.Node;
import misq.p2p.peers.PeerGroup;
import misq.p2p.router.Router;
import misq.p2p.router.gossip.GossipResult;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Preliminary ideas:
 * Use a ephemeral ID (key) for mailbox msg so a receiver can pick the right msg to decode.
 * At initial data request split the request in chunks. E.g. Give me the first 25% of your data to first node,
 * Give me the second 25% of your data to second node, ... with some overlaps as nodes do not have all the same data.
 * The data need to be sorted deterministically.
 * For non-temporary data use age and deliver historical data only on extra demand.
 * At startup give users option to use clear-net for initial sync and switch to tor after that. Might require a restart ;-(.
 * That way the user trades of speed with loss of little privacy (the other nodes learn that that IP uses misq).
 * Probably acceptable trade off for many users. Would be good if the restart could be avoided. Maybe not that hard...
 */
public class DataService implements MessageListener, ConnectionListener {
    private static final long BROADCAST_TIMEOUT = 90;

    private final Node node;
    private final Router router;
    private final Storage storage;
    private final Set<DataListener> dataListeners = new CopyOnWriteArraySet<>();
    private final Map<String, InventoryResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, InventoryRequestHandler> requestHandlerMap = new ConcurrentHashMap<>();

    public DataService(Node node, PeerGroup peerGroup, Storage storage) {
        this.node = node;
        this.storage = storage;

        router = new Router(node, peerGroup);

        router.addMessageListener(this);
        node.addConnectionListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection) {
        if (message instanceof AddDataRequest) {
            AddDataRequest addDataRequest = (AddDataRequest) message;
            if (canAdd(addDataRequest)) {
              /*  Message previousItem = storage.add(addDataRequest.getMapValue());
                if (previousItem == null) {
                    dataListeners.forEach(listener -> listener.onDataAdded(message));
                }*/
            }
        } else if (message instanceof RemoveDataRequest) {
            RemoveDataRequest removeDataRequest = (RemoveDataRequest) message;
            if (canRemove(removeDataRequest)) {
                // Message removedItem = storage.remove(removeDataRequest.getMapKey());
              /*  if (removedItem != null) {
                    dataListeners.forEach(listener -> listener.onDataRemoved(message));
                }*/
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.ConnectionListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnection(Connection connection) {
        addResponseHandler(connection);
    }

    @Override
    public void onDisconnect(Connection connection) {
        String id = connection.getId();
        MapUtils.disposeAndRemove(id, responseHandlerMap);
        MapUtils.disposeAndRemove(id, requestHandlerMap);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<GossipResult> requestAddData() {
        AddDataRequest addDataRequest = new AddDataRequest();
        return router.broadcast(addDataRequest);
    }

    public CompletableFuture<GossipResult> requestRemoveData(Message message) {
        //   RemoveDataRequest removeDataRequest = new RemoveDataRequest(new MapKey(message));
        //  storage.remove(removeDataRequest.getMapKey());
        // return router.broadcast(removeDataRequest);
        return null;
    }

    public CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter) {
        return requestInventory(dataFilter, router.getPeerAddressesForInventoryRequest())
                .whenComplete((requestInventoryResult, throwable) -> {
                    if (requestInventoryResult != null) {
                      /*  storage.add(requestInventoryResult.getInventory())
                                .handle((inventory, error) -> {
                                    if (inventory != null) {
                                        return requestInventoryResult;
                                    } else {
                                        return CompletableFuture.failedFuture(error);
                                    }
                                });*/
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

    CompletableFuture<RequestInventoryResult> requestInventory(DataFilter dataFilter, Address address) {
        long ts = System.currentTimeMillis();
        CompletableFuture<RequestInventoryResult> future = new CompletableFuture<>();
        future.orTimeout(BROADCAST_TIMEOUT, TimeUnit.SECONDS);
        node.getConnection(address)
                .thenCompose(connection -> {
                    InventoryRequestHandler requestHandler = new InventoryRequestHandler(node, connection);
                    requestHandlerMap.put(connection.getId(), requestHandler);
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
      /*  InventoryResponseHandler responseHandler = new InventoryResponseHandler(node,
                connection,
                storage::getInventory,
                () -> responseHandlerMap.remove(connection.getId()));
        responseHandlerMap.put(connection.getId(), responseHandler);*/
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
