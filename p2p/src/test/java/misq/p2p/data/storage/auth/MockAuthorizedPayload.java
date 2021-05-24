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
import misq.p2p.data.NetworkData;
import misq.p2p.data.storage.MetaData;
import misq.p2p.data.storage.auth.authorized.AuthorizedPayload;

import java.security.PublicKey;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Getter
public final class MockAuthorizedPayload extends AuthorizedPayload {
    private final MetaData metaData;

    public MockAuthorizedPayload(NetworkData networkData, byte[] signature, PublicKey publicKey) {
        super(networkData, signature, publicKey);

        this.metaData = new MetaData(TimeUnit.DAYS.toMillis(10), 10000, this.getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        return Set.of("30820122300d06092a864886f70d01010105000382010f003082010a0282010100b2020f519d4538bc7820b6f6134be47ea1b4cdfc11b8e3191caf2c62f9718c0bf538975c86a60a7a3444baa24fd23e85056d81c9f1aae429d316c02d21ac596d895a68574c2b0b7da7c437c598548f6d9d57e42917073c60fc0e2c8ab646df634a7fbde2561ba3f0bf773dca1adcde12fc210d57737d641b9e7a1d3a190857b2b3d1a7e7feadced0d36c2dd48f02096eb445bcd6b27bfa87bfc6310a6ff3a5bf546abda0946b56433c99feda10c68716a22e2c8c6201357dc5dc122c6edd118364d9fc62539c30dfd77cd08990a382da27ce672f228903ffce10000c92e8ef9d114d3cd578f7bccac644ec2f01477c839c3fe2dcd48ff0d79754ccf18d93efd90203010001");
    }
}
