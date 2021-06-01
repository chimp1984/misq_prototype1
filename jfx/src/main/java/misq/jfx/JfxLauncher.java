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

package misq.jfx;

import javafx.application.Application;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class JfxLauncher {
    public static void main(String[] args) {
        JfxLauncher.launch().whenComplete((application, throwable) -> {
            log.error("App launched {}", application);
        });
    }

    public static CompletableFuture<Boolean> launch() {
        new Thread(() -> {
            Thread.currentThread().setName("JfxLauncher");
            Application.launch(JfxApplication.class);
        }).start();
        return JfxApplication.LAUNCH_APP_FUTURE;
    }
}
