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

package misq.p2p.router.gossip;

import lombok.Getter;

@Getter
public class GossipResult {
    private final int numSuccess;
    private final int numFaults;
    private final long duration;

    public GossipResult(int numSuccess, int numFaults, long duration) {
        this.numSuccess = numSuccess;
        this.numFaults = numFaults;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "GossipResult{" +
                "\n     numSuccess=" + numSuccess +
                ",\n     numFaults=" + numFaults +
                ",\n     duration=" + duration +
                "\n}";
    }
}
