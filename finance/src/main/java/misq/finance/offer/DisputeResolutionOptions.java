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
import misq.p2p.NetworkId;

import java.util.Set;

// Information about supported dispute resolution and chosen dispute agent
@Getter
@EqualsAndHashCode
public class DisputeResolutionOptions {
    private final Set<DisputeResolution> disputeResolutions;

    public DisputeResolutionOptions(Set<DisputeResolution> disputeResolutions) {
        this.disputeResolutions = disputeResolutions;
    }

    @Getter
    @EqualsAndHashCode
    public static class DisputeResolution {
        public enum Type {
            MEDIATION,
            ARBITRATION
        }

        private final Type type;
        private final NetworkId networkId;

        public DisputeResolution(Type type, NetworkId networkId) {
            this.type = type;
            this.networkId = networkId;
        }
    }
}
