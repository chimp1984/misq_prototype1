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

package misq.p2p.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.security.SignatureUtil;
import misq.common.util.Hex;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Set;

@Slf4j
@EqualsAndHashCode
@Getter
public abstract class SignedData implements NetworkData {
    private final NetworkData networkData;
    private final byte[] signature;
    private final byte[] publicKeyBytes;
    transient private final PublicKey publicKey;

    public SignedData(NetworkData networkData, byte[] signature, PublicKey publicKey) {
        this.networkData = networkData;
        this.signature = signature;
        this.publicKey = publicKey;
        publicKeyBytes = publicKey.getEncoded();
    }

    @Override
    public boolean isDataInvalid() {
        try {
            return networkData.isDataInvalid() ||
                    !getPermittedPublicKeys().contains(Hex.encode(publicKeyBytes)) ||
                    !SignatureUtil.verify(networkData.serialize(), signature, publicKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }

    public abstract Set<String> getPermittedPublicKeys();
}
