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

package misq.p2p.confidential;

import lombok.extern.slf4j.Slf4j;
import misq.common.ObjectSerializer;
import misq.common.security.ConfidentialData;
import misq.common.security.HybridEncryption;
import misq.p2p.Address;
import misq.p2p.KeyPairRepository;
import misq.p2p.NetworkId;
import misq.p2p.NetworkType;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;
import misq.p2p.node.MessageListener;
import misq.p2p.node.Node;
import misq.p2p.peers.PeerGroup;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
public class ConfidentialMessageService implements MessageListener {
    private final Node node;
    private final PeerGroup peerGroup;
    private final Set<MessageListener> messageListeners = new CopyOnWriteArraySet<>();
    private final KeyPairRepository keyPairRepository;

    public ConfidentialMessageService(Node node, PeerGroup peerGroup, KeyPairRepository keyPairRepository) {
        this.node = node;
        this.peerGroup = peerGroup;
        this.keyPairRepository = keyPairRepository;

        node.addMessageListener(this);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection) {
        if (message instanceof ConfidentialMessage) {
            ConfidentialMessage confidentialMessage = (ConfidentialMessage) message;
            if (confidentialMessage instanceof RelayMessage) {
                RelayMessage relayMessage = (RelayMessage) message;
                Address targetAddress = relayMessage.getTargetAddress();
                // send(message, targetAddress);
            } else {
                ConfidentialData confidentialData = confidentialMessage.getConfidentialData();
                keyPairRepository.findKeyPair(confidentialMessage.getTag()).ifPresent(receiversKeyPair -> {
                    try {
                        byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
                        Message decryptedMessage = (Message) ObjectSerializer.deserialize(decrypted);
                        messageListeners.forEach(listener -> listener.onMessage(decryptedMessage, connection));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public CompletableFuture<Connection> send(Message message, NetworkId networkId, KeyPair myKeyPair)
            throws GeneralSecurityException {
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message.serialize(), networkId.getPublicKey(), myKeyPair);
        ConfidentialMessage confidentialMessage = new ConfidentialMessage(confidentialData, networkId.getTag());
        return node.send(confidentialMessage, networkId.getAddress(node.getNetworkType()));
    }

    public CompletableFuture<Connection> send(Message message, Connection connection,
                                              NetworkId networkId, KeyPair myKeyPair)
            throws GeneralSecurityException {
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message.serialize(), networkId.getPublicKey(), myKeyPair);
        ConfidentialMessage confidentialMessage = new ConfidentialMessage(confidentialData, networkId.getTag());
        return node.send(confidentialMessage, connection);
    }

    public CompletableFuture<Connection> relay(Message message, NetworkId networkId, KeyPair myKeyPair) {
       /*   Set<Connection> connections = getConnectionsWithSupportedNetwork(peerAddress.getNetworkType());
      Connection outboundConnection = CollectionUtil.getRandomElement(connections);
        if (outboundConnection != null) {
            //todo we need 2 diff. pub keys for encryption here
            // ConfidentialMessage inner = seal(message);
            // RelayMessage relayMessage = new RelayMessage(inner, peerAddress);
            // ConfidentialMessage confidentialMessage = seal(relayMessage);
            // return node.send(confidentialMessage, outboundConnection);
        }*/
        return CompletableFuture.failedFuture(new Exception("No connection supporting that network type found."));
    }

    public void shutdown() {
        node.removeMessageListener(this);
        messageListeners.clear();
    }


    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    private Set<Connection> getConnectionsWithSupportedNetwork(NetworkType networkType) {
        return peerGroup.getConnectedPeerByAddress().stream()
                .filter(peer -> peer.getCapability().getSupportedNetworkTypes().contains(networkType))
                .flatMap(peer -> node.findConnection(peer.getAddress()).stream())
                .collect(Collectors.toSet());
    }
}
