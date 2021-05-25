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

package misq.p2p;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class DataNodeBase {
    protected P2pService p2pServiceSeed, p2pService1, p2pService2;

    protected void bootstrap(Set<NetworkConfig> networkConfigsSeed,
                             Set<NetworkConfig> networkConfigsNode1,
                             Set<NetworkConfig> networkConfigsNode2) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        getP2pServiceFuture(networkConfigsSeed).whenComplete((p2pService, e) -> {
            assertNotNull(p2pService);
            this.p2pServiceSeed = p2pService;
            latch.countDown();
        });
        getP2pServiceFuture(networkConfigsNode1).whenComplete((p2pService, e) -> {
            assertNotNull(p2pService);
            this.p2pService1 = p2pService;
            latch.countDown();
        });
        getP2pServiceFuture(networkConfigsNode2).whenComplete((p2pService, e) -> {
            assertNotNull(p2pService);
            this.p2pService2 = p2pService;
            latch.countDown();
        });
        boolean bootstrapped = latch.await(1, TimeUnit.MINUTES);
        assertTrue(bootstrapped);
    }


    protected CompletableFuture<P2pService> getP2pServiceFuture(Set<NetworkConfig> networkConfigs) {
        P2pService p2pService = new P2pService(networkConfigs, k -> null);
        return p2pService.bootstrap().thenApply(result -> p2pService);
    }
}
