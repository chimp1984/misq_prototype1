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

package misq.p2p.confidential;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import misq.common.security.Seal;
import misq.p2p.message.Message;

import java.security.PublicKey;

@EqualsAndHashCode
@Getter
public class ConfidentialMessage implements Message {
    private final Seal seal;
    // We support multiple key pairs, so receiver need to know which key is associated to message
    private final PublicKey receiversPublicKey;

    public ConfidentialMessage(Seal seal, PublicKey receiversPublicKey) {
        this.seal = seal;
        this.receiversPublicKey = receiversPublicKey;
    }

    @Override
    public String toString() {
        return "ConfidentialMessage{" +
                "\n     sealedMessage=" + seal +
                "\n}";
    }
}
