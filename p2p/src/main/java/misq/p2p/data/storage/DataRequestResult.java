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

import lombok.Getter;

@Getter
public class DataRequestResult {
    private final boolean success;
    private boolean publicKeyInvalid, sequenceNrInvalid, noEntry, alreadyRemoved, signatureInvalid;

    public DataRequestResult(boolean success) {
        this.success = success;
    }

    public DataRequestResult publicKeyInvalid() {
        publicKeyInvalid = true;
        return this;
    }

    public DataRequestResult sequenceNrInvalid() {
        sequenceNrInvalid = true;
        return this;
    }


    public DataRequestResult noEntry() {
        noEntry = true;
        return this;
    }

    public DataRequestResult signatureInvalid() {
        signatureInvalid = true;
        return this;
    }

    public DataRequestResult alreadyRemoved() {
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
