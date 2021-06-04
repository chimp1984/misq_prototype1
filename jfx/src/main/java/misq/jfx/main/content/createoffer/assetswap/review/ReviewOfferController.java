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

package misq.jfx.main.content.createoffer.assetswap.review;

import lombok.Getter;
import misq.api.Api;
import misq.finance.offer.OpenOffers;
import misq.jfx.common.Controller;

public class ReviewOfferController implements Controller {
    private final OpenOffers openOffers;
    private ReviewOfferModel model;
    @Getter
    private ReviewOfferView view;

    public ReviewOfferController(Api api) {
        openOffers = api.getOpenOffers();
    }

    @Override
    public void initialize() {
        this.model = new ReviewOfferModel();
        this.view = new ReviewOfferView(model, this);
    }

    @Override
    public void onViewAdded() {
    }

    @Override
    public void onViewRemoved() {
    }

    public void setAskValue(String value) {
        model.setAskValue(value);
    }

    public void onPublish() {
        openOffers.createNewOffer(model.askAmount);
    }
}
