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

package misq.finance.offer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.finance.ProtocolType;
import misq.p2p.NetworkId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode
@Getter
public class Offer {
    private final String id;
    private final long date;
    private final List<? extends ProtocolType> protocolTypes;
    private final NetworkId makerNetworkId;
    private final Optional<DisputeResolutionOptions> disputeResolutionOptions;
    private final Optional<FeeOptions> feeOptions;
    private final Optional<ReputationOptions> reputationOptions;
    private final Optional<TransferOptions> transferOptions;

    public Offer(List<? extends ProtocolType> protocolTypes, NetworkId makerNetworkId) {
        this(protocolTypes, makerNetworkId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Offer(List<? extends ProtocolType> protocolTypes,
                 NetworkId makerNetworkId,
                 Optional<DisputeResolutionOptions> disputeResolutionOptions,
                 Optional<FeeOptions> feeOptions,
                 Optional<ReputationOptions> reputationOptions,
                 Optional<TransferOptions> transferOptions) {
        id = UUID.randomUUID().toString();
        date = System.currentTimeMillis();
        this.protocolTypes = protocolTypes;
        this.makerNetworkId = makerNetworkId;
        this.disputeResolutionOptions = disputeResolutionOptions;
        this.feeOptions = feeOptions;
        this.reputationOptions = reputationOptions;
        this.transferOptions = transferOptions;
    }
}
