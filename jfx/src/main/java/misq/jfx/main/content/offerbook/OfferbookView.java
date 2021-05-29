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
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import misq.jfx.common.ViewWithModel;
import misq.jfx.components.AutoTooltipTableColumn;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class OfferbookView extends ViewWithModel<StackPane, OfferbookViewModel> {
    private final TableView<OfferListItem> tableView;

    private Optional<Scene> scene = Optional.empty();

    public OfferbookView() {
        super(new StackPane(), new OfferbookViewModel());

        // root.setStyle("-fx-background-color: red;");
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableView.setItems(model.offerListItems);
        addPropertyColumn("Price", OfferListItem::getPrice);
        addValueColumn("Amount", OfferListItem::getAmount);
        addValueColumn("Details", OfferListItem::getDetails);
        addValueColumn("Maker", OfferListItem::getMaker);

        root.getChildren().add(tableView);
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
