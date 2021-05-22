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
import misq.common.util.Hex;
import misq.p2p.NetworkData;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class ProtectedData implements Serializable {
    private final NetworkData networkData;
    protected final byte[] hashOfPublicKey;

    public ProtectedData(NetworkData networkData, byte[] hashOfPublicKey) {
        this.networkData = networkData;
        this.hashOfPublicKey = hashOfPublicKey;
    }

    public byte[] getHashOfPublicKey() {
        return hashOfPublicKey;
    }


    @Override
    public String toString() {
        return "ProtectedData{" +
                "\n     networkData=" + networkData +
                ",\n     hashOfPublicKey=" + Hex.encode(hashOfPublicKey) +
                "\n}";
    }
}
