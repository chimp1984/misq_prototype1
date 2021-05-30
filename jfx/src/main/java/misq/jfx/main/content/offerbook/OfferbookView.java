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
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.ViewWithModel;
import misq.jfx.components.AutoTooltipButton;
import misq.jfx.components.AutoTooltipTableColumn;
import misq.jfx.components.AutocompleteComboBox;
import misq.jfx.main.MainView;
import misq.jfx.main.content.createoffer.CreateOfferView;
import misq.jfx.navigation.Navigation;
import org.controlsfx.control.RangeSlider;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class OfferbookView extends ViewWithModel<VBox, OfferbookViewModel> {
    private final TableView<OfferItem> tableView;
    private final HBox toolbar;
    private final AutoTooltipButton createOfferButton;

    public OfferbookView() {
        super(new VBox(), new OfferbookViewModel());
        toolbar = new HBox();
        toolbar.setMinHeight(70);
        toolbar.setMaxHeight(toolbar.getMinHeight());

        RangeSlider slider = new RangeSlider(0, 100, 10, 90);
        //Setting the slider properties
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(10);

        Label askCurrencyLabel = new Label("I want (ask):");
        askCurrencyLabel.setPadding(new Insets(4, 8, 0, 0));

        AutocompleteComboBox<String> askCurrency = new AutocompleteComboBox<>();
        askCurrency.setAutocompleteItems(model.currencies);
        askCurrency.setOnAction(e -> model.onAskCurrencySelected(askCurrency.getSelectionModel().getSelectedItem()));
        askCurrency.getEditor().getStyleClass().add("combo-box-editor-bold");
        askCurrency.getSelectionModel().select(0);
        model.onAskCurrencySelected(askCurrency.getSelectionModel().getSelectedItem());

        VBox askBox = new VBox();
        askBox.getChildren().addAll(askCurrencyLabel, slider);

        Label bidCurrencyLabel = new Label("I give (bid):");
        bidCurrencyLabel.setPadding(new Insets(4, 8, 0, 60));
        AutocompleteComboBox<String> bidCurrency = new AutocompleteComboBox<>();
        bidCurrency.setAutocompleteItems(model.currencies);
        bidCurrency.setOnAction(e -> model.onBidCurrencySelected(bidCurrency.getSelectionModel().getSelectedItem()));
        bidCurrency.getEditor().getStyleClass().add("combo-box-editor-bold");
        bidCurrency.getSelectionModel().select(1);
        model.onBidCurrencySelected(bidCurrency.getSelectionModel().getSelectedItem());

        Button flipButton = new AutoTooltipButton("<- Flip ->");
        HBox.setMargin(flipButton, new Insets(-2, 0, 0, 60));
        flipButton.setOnAction(e -> {
            String ask = askCurrency.getSelectionModel().getSelectedItem();
            String bid = bidCurrency.getSelectionModel().getSelectedItem();
            askCurrency.getSelectionModel().select(bid);
            bidCurrency.getSelectionModel().select(ask);
        });

        createOfferButton = new AutoTooltipButton("Create offer");
        createOfferButton.setOnAction(e -> Navigation.navigateTo(MainView.class, CreateOfferView.class));
        HBox.setMargin(createOfferButton, new Insets(20, 20, 20, 20));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(/*askBox, */askCurrencyLabel, askCurrency, flipButton, bidCurrencyLabel, bidCurrency, spacer, createOfferButton);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        //tableView.setTableMenuButtonVisible(true);

        tableView.setItems(model.sortedItems);
        model.sortedItems.comparatorProperty().bind(tableView.comparatorProperty());
        addValueColumn("Offered amount", OfferItem::getBaseAmountWithMinAmount, Optional.of(OfferItem::compareBaseAmount));
        addPropertyColumn("Price", OfferItem::getPrice, Optional.of(OfferItem::comparePrice));
        addPropertyColumn("Amount to pay", OfferItem::getQuoteAmount, Optional.of(OfferItem::compareQuoteAmount));
        addValueColumn("Details", OfferItem::getTransferOptions, Optional.empty());
        addMakerColumn("Maker", OfferItem::getMakerInfo);
        addTakeOfferColumn("");

        model.marketPrice.addListener(observable -> tableView.sort());
        tableView.sort();
        root.getChildren().addAll(toolbar, tableView);
    }

    private void addMakerColumn(String header, Function<OfferItem, String> valueSupplier) {
        AutoTooltipTableColumn<OfferItem, OfferItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferItem, OfferItem> call(
                            TableColumn<OfferItem, OfferItem> column) {
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
                            public void updateItem(final OfferItem item, boolean empty) {
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

    private void addTakeOfferColumn(String header) {
        AutoTooltipTableColumn<OfferItem, OfferItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferItem, OfferItem> call(
                            TableColumn<OfferItem, OfferItem> column) {
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
                            public void updateItem(final OfferItem item, boolean empty) {
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

    private void onTakeOffer(OfferItem item) {

    }

    private void addValueColumn(String header, Function<OfferItem, String> displayStringSupplier, Optional<Comparator<OfferItem>> optionalComparator) {
        AutoTooltipTableColumn<OfferItem, OfferItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferItem, OfferItem> call(
                            TableColumn<OfferItem, OfferItem> column) {
                        return new TableCell<>() {
                            @Override
                            public void updateItem(final OfferItem item, boolean empty) {
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

    private void addPropertyColumn(String header, Function<OfferItem, StringProperty> valueSupplier, Optional<Comparator<OfferItem>> optionalComparator) {
        AutoTooltipTableColumn<OfferItem, OfferItem> column = new AutoTooltipTableColumn<>(header) {
            {
                setMinWidth(125);
            }
        };
        column.setCellValueFactory((offer) -> new ReadOnlyObjectWrapper<>(offer.getValue()));
        column.setCellFactory(
                new Callback<>() {
                    @Override
                    public TableCell<OfferItem, OfferItem> call(
                            TableColumn<OfferItem, OfferItem> column) {
                        return new TableCell<>() {
                            OfferItem previousItem;

                            @Override
                            public void updateItem(final OfferItem item, boolean empty) {
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
