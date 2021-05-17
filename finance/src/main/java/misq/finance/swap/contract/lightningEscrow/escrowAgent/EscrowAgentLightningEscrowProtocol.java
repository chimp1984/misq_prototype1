package misq.finance.swap.contract.lightningEscrow.escrowAgent;

import misq.finance.contract.ManyPartyContract;
import misq.finance.swap.contract.lightningEscrow.LightningEscrow;
import misq.finance.swap.contract.lightningEscrow.LightningEscrowProtocol;
import misq.p2p.P2pService;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;

import java.util.concurrent.CompletableFuture;

public class EscrowAgentLightningEscrowProtocol extends LightningEscrowProtocol {
    public EscrowAgentLightningEscrowProtocol(ManyPartyContract contract, P2pService p2pService) {
        super(contract, p2pService, null, new LightningEscrow());
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void onMessage(Message message, Connection connection) {
    }
}
