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

package misq.p2p.data.storage.auth.authorized;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.security.SignatureUtil;
import misq.common.util.Hex;
import misq.p2p.data.NetworkData;
import misq.p2p.data.storage.auth.AuthenticatedPayload;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Set;

/**
 * Data which is signed by an authorized key (e.g. Filter, Alert, DisputeAgent...)
 */
@Slf4j
@EqualsAndHashCode
@Getter
public abstract class AuthorizedData implements AuthenticatedPayload {
    private final NetworkData networkData;
    private final byte[] signature;
    private final byte[] authorizedPublicKeyBytes;
    transient private final PublicKey authorizedPublicKey;

    public AuthorizedData(NetworkData networkData, byte[] signature, PublicKey authorizedPublicKey) {
        this.networkData = networkData;
        this.signature = signature;
        this.authorizedPublicKey = authorizedPublicKey;
        authorizedPublicKeyBytes = authorizedPublicKey.getEncoded();
    }

    @Override
    public boolean isDataInvalid() {
        try {
            return networkData.isDataInvalid() ||
                    !getAuthorizedPublicKeys().contains(Hex.encode(authorizedPublicKeyBytes)) ||
                    !SignatureUtil.verify(networkData.serialize(), signature, authorizedPublicKey);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }

    public abstract Set<String> getAuthorizedPublicKeys();
}
