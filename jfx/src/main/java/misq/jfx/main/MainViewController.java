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

package misq.jfx.main;

import lombok.Getter;
import misq.api.Api;
import misq.jfx.common.Controller;
import misq.jfx.main.content.ContentViewController;
import misq.jfx.main.content.offerbook.OfferbookController;
import misq.jfx.main.left.NavigationViewController;
import misq.jfx.main.top.TopPanelController;
import misq.jfx.overlay.OverlayController;

public class MainViewController implements Controller {
    private final Api api;
    private final OverlayController overlayController;
    private MainViewModel model;
    @Getter
    private MainView view;

    public MainViewController(Api api, OverlayController overlayController) {
        this.api = api;
        this.overlayController = overlayController;
    }

    public void initialize() {
        try {
            this.model = new MainViewModel();

            ContentViewController contentViewController = new ContentViewController(api, overlayController);
            contentViewController.initialize();
            NavigationViewController navigationViewController = new NavigationViewController(contentViewController, overlayController);
            navigationViewController.initialize();
            TopPanelController topPanelController = new TopPanelController();
            topPanelController.initialize();

            this.view = new MainView(model,
                    this,
                    contentViewController.getView(),
                    navigationViewController.getView(),
                    topPanelController.getView());

            contentViewController.onNavigationRequest(OfferbookController.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onViewAdded() {

    }

    @Override
    public void onViewRemoved() {

    }
}
