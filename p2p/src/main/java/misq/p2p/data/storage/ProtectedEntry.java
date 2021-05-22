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
import misq.common.util.ObjectSerializer;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class ProtectedEntry implements Serializable {
    private final ProtectedData protectedData;
    private final int sequenceNumber;
    private final long created;


    public ProtectedEntry(ProtectedData protectedData,
                          int sequenceNumber,
                          long created) {
        this.protectedData = protectedData;
        this.sequenceNumber = sequenceNumber;
        this.created = created;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - created) > protectedData.getNetworkData().getMetaData().getTTL();
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }


    @Override
    public String toString() {
        return "ProtectedEntry{" +
                "\n     protectedData=" + protectedData +
                ",\n     sequenceNumber=" + sequenceNumber +
                ",\n     creationTimeStamp=" + created +
                "\n}";
    }

    public byte[] serialize() {
        return ObjectSerializer.serialize(this);
    }
}
