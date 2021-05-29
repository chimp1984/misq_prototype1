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
import misq.jfx.ApplicationModel;
import misq.jfx.JfxLauncher;
import misq.jfx.main.content.offerbook.OfferbookViewModel;
import misq.presentation.Offerbook;

@Slf4j
public class Desktop {
    private ApplicationModel applicationModel;

    public Desktop() {
        launchApplication();
    }

    private void launchApplication() {
        JfxLauncher.launch()
                .whenComplete((applicationModel, throwable) -> {
                    this.applicationModel = applicationModel;
                    init();
                });
    }

    private void init() {
        Offerbook offerbook = new Offerbook();
        applicationModel.connect(OfferbookViewModel.class, new OfferbookDataModel(offerbook));
    }
}
