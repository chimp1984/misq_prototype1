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

package misq.p2p.capability;

import lombok.extern.slf4j.Slf4j;
import misq.common.util.Disposable;
import misq.p2p.NetworkType;
import misq.p2p.endpoint.Address;
import misq.p2p.endpoint.Connection;
import misq.p2p.endpoint.Message;
import misq.p2p.endpoint.MessageListener;

import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class CapabilityResponseHandler implements MessageListener, Disposable {
    private final Connection connection;
    private final Address myAddress;
    private final Set<NetworkType> mySupportedNetworkTypes;
    private final Consumer<Capability> resultHandler;

    public CapabilityResponseHandler(Connection connection,
                                     Address myAddress,
                                     Set<NetworkType> mySupportedNetworkTypes,
                                     Consumer<Capability> resultHandler) {
        this.connection = connection;
        this.myAddress = myAddress;
        this.mySupportedNetworkTypes = mySupportedNetworkTypes;
        this.resultHandler = resultHandler;

        connection.addMessageListener(this);
    }

    public void dispose() {
        connection.removeMessageListener(this);
    }

    @Override
    public void onMessage(Connection connection, Message message) {
        if (message instanceof CapabilityRequest) {
            CapabilityRequest capabilityRequest = (CapabilityRequest) message;
          /*  ConnectionMetaData metaData = connection.getMetaData().get();
            metaData.setPeerAddress(handshakeRequest.getAddress());
            metaData.setSupportedNetworkTypes(handshakeRequest.getSupportedNetworkTypes());
            metaData.handShakeCompleted();*/
            Capability capability = new Capability(myAddress, mySupportedNetworkTypes);
            connection.send(new CapabilityResponse(capability, capabilityRequest.getNonce()));
            connection.removeMessageListener(this);
            resultHandler.accept(capabilityRequest.getCapability());
        }
    }

}
