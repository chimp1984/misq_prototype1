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

package misq.contract.bsqBond.bob;


import lombok.extern.slf4j.Slf4j;
import misq.contract.Contract;
import misq.contract.Network;
import misq.contract.SecurityProvider;
import misq.contract.Transfer;
import misq.contract.bsqBond.BsqBondProtocol;
import misq.contract.bsqBond.alice.AliceCommitmentMessage;
import misq.contract.bsqBond.alice.AliceFundsSentMessage;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class BobBsqBondProtocol extends BsqBondProtocol {
    public BobBsqBondProtocol(Contract contract, Network network, Transfer transfer, SecurityProvider securityProvider) {
        super(contract, network, transfer, securityProvider);
    }

    @Override
    public void onMessage(Object message) {
        if (message instanceof AliceCommitmentMessage) {
            AliceCommitmentMessage bondCommitmentMessage = (AliceCommitmentMessage) message;
            security.verifyBondCommitmentMessage(bondCommitmentMessage)
                    .whenComplete((success, t) -> {
                        setState(State.COMMITMENT_RECEIVED);
                    })
                    .thenCompose(isValid -> security.getCommitment(contract))
                    .thenCompose(commitment -> network.send(new BobCommitmentMessage(commitment), contract.getPeer()))
                    .whenComplete((success, t) -> {
                        setState(State.COMMITMENT_SENT);
                    });
        }
        if (message instanceof AliceFundsSentMessage) {
            AliceFundsSentMessage fundsSentMessage = (AliceFundsSentMessage) message;
            security.verifyFundsSentMessage(fundsSentMessage)
                    .whenComplete((success, t) -> {
                        setState(State.FUNDS_RECEIVED);
                    })
                    .thenCompose(isValid -> transport.sendFunds(contract))
                    .thenCompose(isSent -> network.send(new BobFundsSentMessage(), contract.getPeer()))
                    .whenComplete((success, t) -> {
                        setState(State.FUNDS_SENT);
                    });
        }
    }

    public CompletableFuture<Boolean> start() {
        network.addListener(this);
        setState(State.START);
        return CompletableFuture.completedFuture(true);
    }
}
