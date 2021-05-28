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

package misq.desktop;

import lombok.extern.slf4j.Slf4j;
import misq.jfx.ApplicationRepo;
import misq.jfx.JfxLauncher;
import misq.jfx.main.content.offerbook.OfferbookViewModel;

@Slf4j
public class Desktop {
    private ApplicationRepo applicationRepo;
    private MarketPriceConfig marketPriceConfig;

    public Desktop() {
        launchApplication();
    }

    private void launchApplication() {
        JfxLauncher.launch()
                .whenComplete((applicationRepo, throwable) -> {
                    this.applicationRepo = applicationRepo;
                    init();
                });
    }

    private void init() {
        // OfferbookConfig.setup();
        OfferbookConfig offerbookConfig = new OfferbookConfig();
        applicationRepo.addLifeCycleListener(OfferbookViewModel.class, offerbookConfig);
    }
}
