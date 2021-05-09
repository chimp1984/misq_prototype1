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


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class Execution implements Protocol.Listener {
    private final Contract contract;
    private final Protocol protocol;

    public static Execution from(Factory factory, Role myRole, Network network) {
        Transfer transfer = factory.getTransport();
        SecurityProvider securityProvider = factory.getSecurity();
        Map<Role, Protocol> protocols = new HashMap<>();
        Contract contract = new Contract(protocols, myRole, new Peer(myRole.peer()));
        protocols.put(Role.Alice, factory.getProtocol(contract, network, transfer, securityProvider));
        protocols.put(Role.Bob, factory.getBobProtocol(contract, network, transfer, securityProvider));
        return new Execution(contract);
    }

    public Execution(Contract contract) {
        this.contract = contract;
        protocol = contract.getProtocol(contract.getMyRole());
        protocol.addListener(this);
    }

    public void start() {
        protocol.start();
    }

    @Override
    public void onStateChange(Protocol.State state) {
        log.info("{}: {}", contract.getMyRole().name(), state);
    }
}