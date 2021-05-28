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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.extern.slf4j.Slf4j;
import misq.finance.Asset;
import misq.finance.swap.offer.SwapOffer;
import misq.jfx.common.LifeCycleChangeListener;
import misq.jfx.common.ViewModel;
import misq.jfx.main.content.offerbook.AssetListItem;
import misq.jfx.main.content.offerbook.OfferListItem;
import misq.jfx.main.content.offerbook.OfferbookViewModel;
import misq.jfx.utils.UserThread;
import misq.presentation.OfferbookPresentation;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
public class OfferbookConfig implements LifeCycleChangeListener {
    private static OfferbookConfig config;
    private OfferbookViewModel viewModel;

   /* static void setup() {
        OfferbookViewModel.setListener(new AViewModel.Listener() {
            @Override
            public void onConstructed(ViewModel viewModel) {
                config = new OfferbookConfig((OfferbookViewModel) viewModel);
                config.onConstructed(viewModel);
            }

            @Override
            public void onInitialized() {
                config.onInitialized();
            }

            @Override
            public void onActivated() {
                config.onActivated();
            }

            @Override
            public void onDeactivated() {
                config.onDeactivated();
            }

            @Override
            public void onDestructed() {
                config.onDestructed();
                config = null;
            }
        });
    }*/

    private OfferbookPresentation presentation;

    public OfferbookConfig() {

    }

    @Override
    public void onConstructed(ViewModel viewModel) {
        this.viewModel = (OfferbookViewModel) viewModel;
        presentation = new OfferbookPresentation();
        presentation.setOffersConsumer(offers -> {
            List<OfferListItem> offerListItems = offers.stream().map(offer -> getOfferListItem((SwapOffer) offer)).collect(Collectors.toList());
            this.viewModel.onOfferListItemsChange(offerListItems);
        });
    }

    @Override
    public void onInitialized() {
        presentation.onInitialized();
    }

    @Override
    public void onActivated() {
    }

    @Override
    public void onDeactivated() {
    }

    @Override
    public void onDestructed() {
    }

    private OfferListItem getOfferListItem(SwapOffer offer) {
        String maker = offer.getMakerAddress().toString();
        AssetListItem askAssetListItem = toAssetListItem(offer.getAskAsset());
        AssetListItem bidAssetListItem = toAssetListItem(offer.getBidAsset());
        String amount = String.valueOf(askAssetListItem.getAmount() / 1000000d).substring(0, 3);
        String details = "Zelle/Multisig";

        StringProperty price = new SimpleStringProperty("");
        UserThread.runPeriodically(() -> {
            double rand = new Random().nextInt(10000) / 10000d;
            double marketPrice = 50000 + 1000 * rand;
            double percentageBasedPrice = 0.02;
            double percentagePrice = marketPrice * (1 + percentageBasedPrice);
            DecimalFormat df = new DecimalFormat("#.##");
            String formatted = df.format(percentagePrice);
            price.set(formatted);
        }, 1000);

        return new OfferListItem(amount, price, maker, details);
    }

    private AssetListItem toAssetListItem(Asset bidAsset) {
        return new AssetListItem(bidAsset.getCode(),
                bidAsset.isBase(),
                bidAsset.getAmount(),
                bidAsset.getTransferTypes().stream().map(Enum::name).collect(Collectors.toList()));
    }
}
