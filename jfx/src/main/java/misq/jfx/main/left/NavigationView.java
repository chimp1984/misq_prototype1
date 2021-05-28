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

package misq.jfx.main.left;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import misq.jfx.components.AutoTooltipToggleButton;
import misq.jfx.main.MainView;
import misq.jfx.main.content.accounts.AccountsView;
import misq.jfx.main.content.funds.FundsView;
import misq.jfx.main.content.markets.MarketsView;
import misq.jfx.main.content.offerbook.OfferbookView;
import misq.jfx.main.content.settings.SettingsView;
import misq.jfx.main.content.trades.TradesView;
import misq.jfx.navigation.Navigation;

public class NavigationView extends VBox {
    private final ToggleGroup navButtons = new ToggleGroup();

    public NavigationView() {
        setMaxWidth(337);
        setMinWidth(337);
        setPadding(new Insets(0, 0, 0, 20));

        NavButton markets = new NavButton(MarketsView.class, "Markets");
        NavButton offerBook = new NavButton(OfferbookView.class, "Offerbook");
        NavButton trades = new NavButton(TradesView.class, "Trades");
        NavButton funds = new NavButton(FundsView.class, "Funds");
        NavButton accounts = new NavButton(AccountsView.class, "Accounts");
        NavButton settings = new NavButton(SettingsView.class, "SettingsView");

        getChildren().addAll(markets, offerBook, trades, funds, accounts, settings);

        Navigation.addListener((viewPath, data) -> {
            if (viewPath.size() != 2 || viewPath.indexOf(MainView.class) != 0) {
                return;
            }

            Class<? extends Node> tip = viewPath.tip();
            navButtons.getToggles().stream()
                    .filter(toggle -> tip == ((NavButton) toggle).target)
                    .forEach(toggle -> toggle.setSelected(true));
        });


    }


    private class NavButton extends AutoTooltipToggleButton {
        final Class<? extends Node> target;

        NavButton(Class<? extends Node> target, String title) {
            super(title);
            this.target = target;
            this.setToggleGroup(navButtons);
            this.getStyleClass().add("navigation-button");
            this.selectedProperty().addListener((ov, oldValue, newValue) -> this.setMouseTransparent(newValue));
            this.setOnAction(e -> Navigation.navigateTo(MainView.class, target));
        }
    }
}
