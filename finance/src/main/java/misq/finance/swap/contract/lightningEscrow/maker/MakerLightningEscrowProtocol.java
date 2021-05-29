package misq.finance.swap.contract.lightningEscrow.maker;

import misq.finance.contract.AssetTransfer;
import misq.finance.contract.ManyPartyContract;
import misq.finance.swap.contract.lightningEscrow.LightningEscrow;
import misq.finance.swap.contract.lightningEscrow.LightningEscrowProtocol;
import misq.p2p.P2pService;
import misq.p2p.message.Message;
import misq.p2p.node.Connection;

import java.util.concurrent.CompletableFuture;

public class MakerLightningEscrowProtocol extends LightningEscrowProtocol {
    public MakerLightningEscrowProtocol(ManyPartyContract contract, P2pService p2pService) {
        super(contract, p2pService, new AssetTransfer.Manual(), new LightningEscrow());
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void onMessage(Message message, Connection connection) {
    }
}
