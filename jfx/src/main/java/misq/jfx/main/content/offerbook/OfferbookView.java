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

package misq.jfx.main.content.offerbook;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.ViewWithModelAndController;
import misq.jfx.components.controls.AutoTooltipButton;
import misq.jfx.components.controls.AutoTooltipSlideToggleButton;
import misq.jfx.components.controls.AutoTooltipTableColumn;
import misq.jfx.components.controls.AutocompleteComboBox;
import misq.jfx.main.MainView;
import misq.jfx.main.content.createoffer.CreateOfferView;
import misq.jfx.navigation.Navigation;
import misq.presentation.offer.OfferListItem;
import misq.presentation.offer.OfferbookController;
import misq.presentation.offer.OfferbookModel;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class OfferbookView extends ViewWithModelAndController<VBox, OfferbookController, OfferbookModel> {
    private TableView<OfferListItem> tableView;
    private RangeSliderBox baseAmountSliderBox, priceSliderBox;
    private AutocompleteComboBox<String> askCurrencyComboBox, bidCurrencyComboBox;
    private AutoTooltipButton flipButton;
    private AutoTooltipSlideToggleButton showAmountPriceFilterToggle;
    private HBox amountPriceFilterBox;
    private AutoTooltipButton createOfferButton;

    public OfferbookView() {
        super(new VBox(), OfferbookController.class);

        setupView();
        configModel();
        configController();
    }

    private void setupView() {
        Label askCurrencyLabel = new Label("I want (ask):");
        askCurrencyLabel.setPadding(new Insets(4, 8, 0, 0));

        askCurrencyComboBox = new AutocompleteComboBox<>();
        askCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        flipButton = new AutoTooltipButton("<- Flip ->");

        Label bidCurrencyLabel = new Label("I give (bid):");
        bidCurrencyLabel.setPadding(new Insets(4, 8, 0, 60));
        bidCurrencyComboBox = new AutocompleteComboBox<>();
        bidCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        HBox.setMargin(flipButton, new Insets(-2, 0, 0, 60));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        createOfferButton = new AutoTooltipButton("Create offer");
        HBox.setMargin(createOfferButton, new Insets(20, 20, 20, 20));

        HBox currencySelectionBox = new HBox();
        currencySelectionBox.setMinHeight(70);
        currencySelectionBox.setMaxHeight(currencySelectionBox.getMinHeight());
        currencySelectionBox.getChildren().addAll(askCurrencyLabel, askCurrencyComboBox, flipButton, bidCurrencyLabel,
                bidCurrencyComboBox, spacer, createOfferButton);

        showAmountPriceFilterToggle = new AutoTooltipSlideToggleButton("Filter by amount and price");
        showAmountPriceFilterToggle.setTextAlignment(TextAlignment.LEFT);

        amountPriceFilterBox = new HBox();
        amountPriceFilterBox.setSpacing(80);
        amountPriceFilterBox.setPadding(new Insets(50, 0, 0, 0));

        baseAmountSliderBox = new RangeSliderBox("Filter by BTC amount", 300, model, controller);
        priceSliderBox = new RangeSliderBox("Filter by price", 300, model, controller);
        amountPriceFilterBox.getChildren().addAll(baseAmountSliderBox, priceSliderBox);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        addValueColumn("Offered amount", OfferListItem::getBaseAmountWithMinAmount, Optional.of(OfferListItem::compareBaseAmount));
        addPropertyColumn("Price", OfferListItem::getPrice, Optional.of(OfferListItem::comparePrice));
        addPropertyColumn("Amount to pay", OfferListItem::getQuoteAmount, Optional.of(OfferListItem::compareQuoteAmount));
        addValueColumn("Details", OfferListItem::getTransferOptions, Optional.empty());
        addMakerColumn("Maker");
        addTakeOfferColumn("");

        root.getChildren().addAll(currencySelectionBox, showAmountPriceFilterToggle, amountPriceFilterBox, tableView);

    }

    private void configModel() {
        askCurrencyComboBox.setAutocompleteItems(model.getCurrencies());
        askCurrencyComboBox.getSelectionModel().select(model.getSelectedAskCurrency().get());

        bidCurrencyComboBox.setAutocompleteItems(model.getCurrencies());
        bidCurrencyComboBox.getSelectionModel().select(model.getSelectedBidCurrency().get());

        amountPriceFilterBox.visibleProperty().bind(model.getAmountPriceFilterVisible());
        showAmountPriceFilterToggle.selectedProperty().bind(model.getAmountPriceFilterVisible());

        model.getSortedItems().comparatorProperty().bind(tableView.comparatorProperty());
        model.getMarketPrice().addListener(observable -> tableView.sort());
        tableView.setItems(model.getSortedItems());
        tableView.sort();
    }

    private void configController() {
        flipButton.setOnAction(e -> {
            String ask = askCurrencyComboBox.getSelectionModel().getSelectedItem();
            String bid = bidCurrencyComboBox.getSelectionModel().getSelectedItem();
            askCurrencyComboBox.getSelectionModel().select(bid);
            bidCurrencyComboBox.getSelectionModel().select(ask);

        });
        askCurrencyComboBox.setOnAction(e -> controller.onSelectAskCurrency(askCurrencyComboBox.getSelectionModel().getSelectedItem()));
        bidCurrencyComboBox.setOnAction(e -> controller.onSelectBidCurrency(bidCurrencyComboBox.getSelectionModel().getSelectedItem()));
        createOfferButton.setOnAction(e -> {
            controller.onCreateOffer();
            Navigation.navigateTo(MainView.class, CreateOfferView.class);
        });
    }

    @Override
    public void onViewAdded() {
        baseAmountSliderBox.onViewAdded();
        priceSliderBox.onViewAdded();
    }

    private void addMakerColumn(String header) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            ImageView iconView = new ImageView();
                            AutoTooltipButton button = new AutoTooltipButton("Show details");

                            {
                                button.setGraphic(iconView);
                                button.setMinWidth(200);
                                button.setMaxWidth(200);
                                button.setGraphicTextGap(10);
                            }

                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    button.setOnAction(e -> onTakeOffer(item));
                                    setPadding(new Insets(0, 15, 0, 0));
                                    setGraphic(button);
                                } else {
                                    button.setOnAction(null);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
        tableView.getColumns().add(column);
    }

    private void addTakeOfferColumn(String header) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            ImageView iconView = new ImageView();
                            AutoTooltipButton button = new AutoTooltipButton("Take offer");

                            {
                                button.setGraphic(iconView);
                                button.setMinWidth(200);
                                button.setMaxWidth(200);
                                button.setGraphicTextGap(10);
                            }

                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    // setText(valueSupplier.apply(item));
                                    button.setOnAction(e -> onTakeOffer(item));
                                    setPadding(new Insets(0, 15, 0, 0));
                                    setGraphic(button);
                                } else {
                                    button.setOnAction(null);
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                });
        tableView.getColumns().add(column);
    }

    private void onTakeOffer(OfferListItem item) {

    }

    private void addValueColumn(String header, Function<OfferListItem, String> displayStringSupplier, Optional<Comparator<OfferListItem>> optionalComparator) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    setText(displayStringSupplier.apply(item));
                                } else {
                                    setText("");
                                }
                            }
                        };
                    }
                });
        optionalComparator.ifPresent(comparator -> {
            column.setSortable(true);
            column.setComparator(comparator);
        });

        tableView.getColumns().add(column);
    }

    private void addPropertyColumn(String header, Function<OfferListItem, StringProperty> valueSupplier, Optional<Comparator<OfferListItem>> optionalComparator) {
        AutoTooltipTableColumn<OfferListItem, OfferListItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferListItem, OfferListItem> call(
                            TableColumn<OfferListItem, OfferListItem> column) {
                        return new TableCell<>() {
                            OfferListItem previousItem;

                            @Override
                            public void updateItem(final OfferListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    if (previousItem != null) {
                                        previousItem.isVisible(false);
                                    }
                                    previousItem = item;

                                    item.isVisible(true);
                                    textProperty().bind(valueSupplier.apply(item));
                                } else {
                                    if (previousItem != null) {
                                        previousItem.isVisible(false);
                                        previousItem = null;
                                    }
                                    textProperty().unbind();
                                    setText("");
                                }
                            }
                        };
                    }
                });
        tableView.getColumns().add(column);
    }
}
