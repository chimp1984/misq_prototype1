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
import misq.common.util.Hex;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.Arrays;

@Getter
@EqualsAndHashCode
@Slf4j
public class AddProtectedDataRequest implements DataTransaction, Serializable {
    protected final ProtectedEntry entry;
    protected final byte[] signature;         // 256 bytes
    protected final byte[] ownerPublicKeyBytes; // 294 bytes
    transient protected final PublicKey ownerPublicKey;

    public AddProtectedDataRequest(ProtectedEntry entry,
                                   byte[] signature,
                                   PublicKey ownerPublicKey) {
        this(entry,
                signature,
                ownerPublicKey.getEncoded(),
                ownerPublicKey);
    }

    protected AddProtectedDataRequest(ProtectedEntry entry,
                                      byte[] signature,
                                      byte[] ownerPublicKeyBytes,
                                      PublicKey ownerPublicKey) {
        this.entry = entry;
        this.ownerPublicKeyBytes = ownerPublicKeyBytes;
        this.ownerPublicKey = ownerPublicKey;
        this.signature = signature;
    }


    public boolean isSignatureInvalid() {
        try {
            return !SignatureUtil.verify(entry.serialize(), signature, ownerPublicKey);
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isPublicKeyInvalid() {
        try {
            return !Arrays.equals(entry.getProtectedData().getHashOfPublicKey(), DigestUtil.sha256(ownerPublicKeyBytes));
        } catch (Exception e) {
            return true;
        }
    }

    public String getFileName() {
        return entry.getProtectedData().getNetworkData().getMetaData().getFileName();
    }

    @Override
    public int getSequenceNumber() {
        return entry.getSequenceNumber();
    }

    @Override
    public long getCreated() {
        return entry.getCreated();
    }

    public int getMaxSizeInBytes() {
        return entry.getProtectedData().getNetworkData().getMetaData().getMaxSizeInBytes();
    }

    public MetaData getMetaData() {
        return entry.getProtectedData().getNetworkData().getMetaData();
    }

    @Getter
    public static class Result {
        private final boolean success;
        private boolean publicKeyInvalid, sequenceNrInvalid, signatureInvalid,
                dataInvalid, expired;
        private Exception exception;

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


        public Result signatureInvalid() {
            signatureInvalid = true;
            return this;
        }

        public Result expired() {
            expired = true;
            return this;
        }

        public Result dataInvalid() {
            dataInvalid = true;
            return this;
        }

        @Override
        public String toString() {
            return "AddDataResult{" +
                    "\n     success=" + success +
                    ",\n     publicKeyInvalid=" + publicKeyInvalid +
                    ",\n     sequenceNrInvalid=" + sequenceNrInvalid +
                    ",\n     signatureInvalid=" + signatureInvalid +
                    ",\n     dataInvalid=" + dataInvalid +
                    ",\n     expired=" + expired +
                    ",\n     exception=" + exception +
                    "\n}";
        }
    }

    @Override
    public String toString() {
        return "AddProtectedDataRequest{" +
                "\n     entry=" + entry +
                ",\n     signature=" + Hex.encode(signature) +
                ",\n     ownerPublicKeyBytes=" + Hex.encode(ownerPublicKeyBytes) +
                "\n}";
    }
}
