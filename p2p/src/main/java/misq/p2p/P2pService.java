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

package misq.p2p;

import com.google.common.collect.Sets;
import misq.p2p.data.filter.DataFilter;
import misq.p2p.data.inventory.RequestInventoryResult;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.MessageListener;
import misq.p2p.node.proxy.GetServerSocketResult;
import misq.p2p.router.gossip.GossipResult;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface P2pService {
    CompletableFuture<Boolean> initializeServer(BiConsumer<GetServerSocketResult, Throwable> resultHandler);

    CompletableFuture<Boolean> bootstrap();

    //todo just temp until api is more stable
    default CompletableFuture<Connection> confidentialSend(Message message, Address peerAddresses) {
        CompletableFuture<Connection> connectionCompletableFuture = null;
        try {
            connectionCompletableFuture = confidentialSend(message, Sets.newHashSet(peerAddresses), null, null);
        } catch (GeneralSecurityException ignore) {
        }
        return connectionCompletableFuture;
    }

    CompletableFuture<Connection> confidentialSend(Message message, Set<Address> peerAddresses,
                                                   PublicKey peersPublicKey, KeyPair myKeyPair) throws GeneralSecurityException;

    void requestAddData(Message message,
                        Consumer<GossipResult> resultHandler);

    void requestRemoveData(Message message,
                           Consumer<GossipResult> resultHandler);

    void requestInventory(DataFilter dataFilter,
                          Consumer<RequestInventoryResult> resultHandler);

    void addMessageListener(MessageListener messageListener);

    void removeMessageListener(MessageListener messageListener);

    void shutdown();

    Optional<Address> findMyAddress(NetworkType networkType);

}
