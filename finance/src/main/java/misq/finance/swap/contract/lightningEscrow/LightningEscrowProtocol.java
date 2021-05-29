package misq.finance.swap.contract.lightningEscrow;

import misq.account.AssetTransfer;
import misq.finance.contract.ManyPartyContract;
import misq.finance.contract.ManyPartyProtocol;
import misq.finance.contract.Protocol;
import misq.finance.contract.SecurityProvider;
import misq.p2p.P2pService;

public abstract class LightningEscrowProtocol extends ManyPartyProtocol {
    public enum State implements Protocol.State {
        START,
        START_MANUAL_PAYMENT,
        MANUAL_PAYMENT_STARTED;
    }

    private final AssetTransfer transport;
    private final LightningEscrow security;

    public LightningEscrowProtocol(ManyPartyContract contract, P2pService p2pService, AssetTransfer assetTransfer, SecurityProvider securityProvider) {
        super(contract, p2pService);
        transport = assetTransfer;
        security = (LightningEscrow) securityProvider;

        if (assetTransfer instanceof AssetTransfer.Manual) {
            ((AssetTransfer.Manual) assetTransfer).addListener(this::onStartManualPayment);
            addListener(state -> {
                if (state == State.MANUAL_PAYMENT_STARTED) {
                    ((AssetTransfer.Manual) assetTransfer).onManualPaymentStarted();
                }
            });
        }
    }

    private void onStartManualPayment() {
    }

    public void onManualPaymentStarted() {
    }
}
