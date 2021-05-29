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
    private final TableView<OfferListItem> tableView;
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

        tableView.setItems(model.offerListItems);
        addPropertyColumn("Price", OfferListItem::getPrice);
        addValueColumn("Amount", OfferListItem::getAmount);
        addValueColumn("Details", OfferListItem::getDetails);
        addValueColumn("Maker", OfferListItem::getMaker);

        root.getChildren().addAll(toolbar, tableView);
    }

    private void addValueColumn(String header, Function<OfferListItem, String> valueSupplier) {
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

    private void addPropertyColumn(String header, Function<OfferListItem, StringProperty> valueSupplier) {
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
