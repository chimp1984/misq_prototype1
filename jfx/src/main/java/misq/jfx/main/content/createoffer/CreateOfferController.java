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

package misq.jfx.main.content.createoffer;

import lombok.Getter;
import misq.api.Api;
import misq.jfx.common.Controller;
import misq.jfx.common.View;
import misq.jfx.main.content.createoffer.assetswap.amounts.SetAmountsController;
import misq.jfx.main.content.createoffer.assetswap.review.ReviewOfferController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CreateOfferController implements Controller {
    private final Api api;
    private CreateOfferModel model;
    @Getter
    private CreateOfferView view;
    private final Map<Class<? extends Controller>, Controller> map = new ConcurrentHashMap<>();

    public CreateOfferController(Api api) {
        this.api = api;
    }

    @Override
    public void initialize() {
        this.model = new CreateOfferModel();
        this.view = new CreateOfferView(model, this);
        addController(new SetAmountsController());
        addController(new ReviewOfferController(api));

        //  Controller controller = map.get(SetAmountsController.class);
        Controller controller = map.get(ReviewOfferController.class);
        controller.initialize();
        View view = controller.getView();
        model.selectView(view);
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }

    public void onNavigateBack() {

    }

    public void onNavigateNext() {

    }

    private void addController(Controller controller) {
        map.put(controller.getClass(), controller);
    }
}
