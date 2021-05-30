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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import misq.jfx.main.MainView;
import misq.jfx.main.content.createoffer.CreateOfferView;
import misq.jfx.navigation.Navigation;

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

        createOfferButton = new AutoTooltipButton("Create offer");
        createOfferButton.setOnAction(e -> Navigation.navigateTo(MainView.class, CreateOfferView.class));
        HBox.setMargin(createOfferButton, new Insets(20, 20, 20, 20));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(spacer, createOfferButton);

        tableView = new TableView<>();
        VBox.setVgrow(tableView, Priority.ALWAYS);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableView.setItems(model.offerItems);
        addPropertyColumn("Price", OfferItem::getPrice);
        addValueColumn("Amount BTC", OfferItem::getBaseAmountWithMinAmount);
        addPropertyColumn("Amount USD", OfferItem::getQuoteAmount);
        addValueColumn("Details", OfferItem::getTransferOptions);
        addMakerColumn("Maker", OfferItem::getMakerInfo);
        addTakeOfferColumn("");

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

    private void addValueColumn(String header, Function<OfferItem, String> valueSupplier) {
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
                                    setText(valueSupplier.apply(item));
                                } else {
                                    setText("");
                                }
                            }
                        };
                    }
                });
        tableView.getColumns().add(column);
    }

    private void addPropertyColumn(String header, Function<OfferItem, StringProperty> valueSupplier) {
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
