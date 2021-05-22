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

package misq.p2p.data.storage;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ProtectedEntry implements MapValue {
    private final ProtectedData protectedData;
    private final int sequenceNumber;
    private final long creationTimeStamp;

    public ProtectedEntry(ProtectedData protectedData,
                          int sequenceNumber,
                          long creationTimeStamp) {
        this.protectedData = protectedData;
        this.sequenceNumber = sequenceNumber;
        this.creationTimeStamp = creationTimeStamp;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - creationTimeStamp) > protectedData.getNetworkData().getTTL();
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }
}
