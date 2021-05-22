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
import lombok.extern.slf4j.Slf4j;
import misq.common.security.DigestUtil;
import misq.common.security.SignatureUtil;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
@Slf4j
public class RefreshProtectedDataRequest implements Serializable {
    protected final String storageFileName;
    protected final byte[] hash;
    protected final byte[] ownerPublicKeyBytes; // 442 bytes
    transient protected final PublicKey ownerPublicKey;
    protected final int sequenceNumber;
    protected final byte[] signature;         // 47 bytes

    public RefreshProtectedDataRequest(String storageFileName,
                                       byte[] hash,
                                       PublicKey ownerPublicKey,
                                       int sequenceNumber,
                                       byte[] signature) {
        this(storageFileName,
                hash,
                ownerPublicKey.getEncoded(),
                ownerPublicKey,
                sequenceNumber,
                signature);
    }

    protected RefreshProtectedDataRequest(String storageFileName,
                                          byte[] hash,
                                          byte[] ownerPublicKeyBytes,
                                          PublicKey ownerPublicKey,
                                          int sequenceNumber,
                                          byte[] signature) {
        this.storageFileName = storageFileName;
        this.hash = hash;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.sequenceNumber = sequenceNumber;
        this.signature = signature;
    }


    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(hash, signature, ownerPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isPublicKeyInvalid(ProtectedData protectedData) {
        try {
            return !Arrays.equals(protectedData.getHashOfPublicKey(), DigestUtil.sha256(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isSequenceNrInvalid(long seqNumberFromMap) {
        return sequenceNumber <= seqNumberFromMap;
    }

    @Getter
    public static class Result {
        private final boolean success;
        private boolean publicKeyInvalid, sequenceNrInvalid, noEntry, alreadyRemoved, signatureInvalid;

        public Result(boolean success) {
            this.success = success;
        }

        public Result publicKeyInvalid() {
            publicKeyInvalid = true;
            return this;
        }

        public Result sequenceNrInvalid() {
            sequenceNrInvalid = true;
            return this;
        }


        public Result noEntry() {
            noEntry = true;
            return this;
        }

        public Result signatureInvalid() {
            signatureInvalid = true;
            return this;
        }

        public Result alreadyRemoved() {
            alreadyRemoved = true;
            return this;
        }

        @Override
        public String toString() {
            return "RemoveDataResult{" +
                    "\n     success=" + success +
                    ",\n     publicKeyInvalid=" + publicKeyInvalid +
                    ",\n     sequenceNrInvalid=" + sequenceNrInvalid +
                    ",\n     noEntry=" + noEntry +
                    ",\n     alreadyRemoved=" + alreadyRemoved +
                    ",\n     signatureInvalid=" + signatureInvalid +
                    "\n}";
        }
    }
}
