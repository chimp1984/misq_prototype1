package misq.finance.contract.sharedState;

import misq.finance.swap.contract.multiSig.MultiSig;
import org.junit.Test;

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
    }
}
