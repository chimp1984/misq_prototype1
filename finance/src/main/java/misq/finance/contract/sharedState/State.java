package misq.finance.contract.sharedState;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface State {
    String[] parties();
}
