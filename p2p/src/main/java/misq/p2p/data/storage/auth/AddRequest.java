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

package misq.p2p.data.storage.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import misq.common.security.DigestUtil;
import misq.common.security.SignatureUtil;
import misq.common.util.Hex;
import misq.p2p.data.storage.MetaData;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
@Slf4j
public class AddRequest implements AuthenticatedDataRequest, Serializable {
    protected final AuthenticatedData authenticatedData;
    protected final byte[] signature;         // 256 bytes
    protected final byte[] ownerPublicKeyBytes; // 294 bytes
    transient protected final PublicKey ownerPublicKey;

    public AddRequest(AuthenticatedData authenticatedData, byte[] signature, PublicKey ownerPublicKey) {
        this(authenticatedData,
                signature,
                ownerPublicKey.getEncoded(),
                ownerPublicKey);
    }

    protected AddRequest(AuthenticatedData authenticatedData,
                         byte[] signature,
                         byte[] ownerPublicKeyBytes,
                         PublicKey ownerPublicKey) {
        this.authenticatedData = authenticatedData;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.signature = signature;
    }


    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(authenticatedData.serialize(), signature, ownerPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isPublicKeyInvalid() {
        try {
            return !Arrays.equals(authenticatedData.getHashOfPublicKey(), DigestUtil.sha256(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public String getFileName() {
        return authenticatedData.getPayload().getMetaData().getFileName();
    }

    @Override
    public int getSequenceNumber() {
        return authenticatedData.getSequenceNumber();
    }

    @Override
    public long getCreated() {
        return authenticatedData.getCreated();
    }

    public MetaData getMetaData() {
        return authenticatedData.getPayload().getMetaData();
    }

    @Override
    public String toString() {
        return "AddProtectedDataRequest{" +
                "\n     entry=" + authenticatedData +
                ",\n     signature=" + Hex.encode(signature) +
                ",\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                "\n}";
    }

}
