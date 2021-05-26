package misq.finance.contract.sharedState;

import misq.chain.Bitcoind;
import misq.finance.swap.contract.multiSig.MultiSig;
import org.junit.Test;

import java.util.Map;

import static misq.finance.contract.SecurityProvider.Unit.UNIT;
import static misq.finance.contract.sharedState.SharedStateFactory.fireEvent;
import static misq.finance.contract.sharedState.SharedStateFactory.when;

public class SharedStateFactoryTest {
    @Test
    public void testNew() {
//        var factory = new SharedStateFactory<>(LightningEscrow.SharedState.class);
        var factory = new SharedStateFactory<>(MultiSig.SharedState.class);

        System.out.println();
        System.out.println(factory.getParties());
        System.out.println();
        factory.getDependencyMultimap().asMap().forEach((id, list) -> System.out.println(id + " -> " + list));
        System.out.println();
        factory.getActorMap().forEach((id, party) -> System.out.println(id + " -> " + party));
        System.out.println();
        factory.getSupplierMap().forEach((id, party) -> System.out.println(id + " -> " + party));
        System.out.println();
        factory.getEventObserverMap().forEach((id, party) -> System.out.println(id + " -> " + party));
        System.out.println();
        factory.getDeclaredAccessConditionMap().forEach((id, cond) -> System.out.println(id + " -> " + cond));
        System.out.println();
        factory.getAccessConditionMap().forEach((id, cond) -> System.out.println(id + " -> " + cond));
        System.out.println();

        var wallet = new Bitcoind();
        var makerState = factory.create("maker", state -> {
            when(state.makerWallet()).thenReturn(wallet);
            when(state.makerEscrowPublicKey()).thenReturn("makerPubKey");
            when(state.makerPayoutAddress()).thenReturn("makerPayoutAddress");
        });
        var takerState = factory.create("taker", state -> {
            when(state.takerWallet()).thenReturn(wallet);
            when(state.takerEscrowPublicKey()).thenReturn("takerPubKey");
            when(state.takerPayoutAddress()).thenReturn("takerPayoutAddress");
        });

        Map<String, ?> message;

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println(takerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM taker TO maker: " + toString(message = takerState.sendMessage("maker")));
        System.out.println();
        makerState.receiveMessage("taker", message);

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println(takerState);
        System.out.println();

        System.out.println("FIRING EVENTS: takerSeesDepositTxInMempool, makerSeesDepositTxInMempool, makerStartsCountercurrencyPayment");
        System.out.println();
        fireEvent(takerState::takerSeesDepositTxInMempool, "publishedDepositTx");
        fireEvent(makerState::makerSeesDepositTxInMempool, "publishedDepositTx");
        fireEvent(makerState::makerStartsCountercurrencyPayment, UNIT);

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println("FIRING EVENTS: takerConfirmsCountercurrencyPayment, takerSeesPayoutTxInMempool, makerSeesPayoutTxInMempool");
        System.out.println();
        fireEvent(takerState::takerConfirmsCountercurrencyPayment, UNIT);
        fireEvent(takerState::takerSeesPayoutTxInMempool, "publishedPayoutTx");
        fireEvent(makerState::makerSeesPayoutTxInMempool, "publishedPayoutTx");

        System.out.println(takerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM taker TO maker: " + toString(message = takerState.sendMessage("maker")));
        System.out.println();
        makerState.receiveMessage("taker", message);

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println(makerState.makerDepositTxInputs());
        System.out.println();
//        System.out.println(makerState.depositTx());
    }

    private static String toString(Map<String, ?> message) {
        var sb = new StringBuilder("{");
        var len = sb.length();
        message.forEach((name, value) ->
                sb.append(len == sb.length() ? "" : ",").append("\n  ").append(name).append(" = ").append(value)
        );
        return sb.append("\n}").toString();
    }
}
