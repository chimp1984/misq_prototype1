package misq.finance.contract;

import lombok.Getter;
import misq.finance.ProtocolType;
import misq.finance.Role;

import java.util.Map;

@Getter
public class ManyPartyContract extends Contract {
    private final Map<Role, Party> partyMap;

    public ManyPartyContract(ProtocolType protocolType, Role myRole, Map<Role, Party> partyMap) {
        super(protocolType, myRole);
        this.partyMap = Map.copyOf(partyMap);
    }
}
