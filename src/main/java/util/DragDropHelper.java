package util;

import model.Game;
import model.Player;
import model.Role;
import model.Team;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import java.util.function.Supplier;

public class DragDropHelper {

    private static final String MIME_PLAYER = "application/x-esport-player";

    private final DataStore store;
    private DragContext context;

    public DragDropHelper(DataStore store) {
        this.store = store;
        this.context = new DragContext();
    }

    public void configure(ListView<Player> listView,
                          Supplier<DragGame> gameSupplier,
                          Supplier<Team> teamSupplier) {

        listView.setCellFactory(lv -> new PlayerCell(gameSupplier));

        listView.setOnDragDetected((MouseEvent e) -> {
            Player selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            DragGame dg = gameSupplier.get();
            context.player = selected;
            context.sourceGame = dg != null ? dg.game : null;
            context.sourceTeam = teamSupplier.get();

            ClipboardContent content = new ClipboardContent();
            content.putString(selected.getId());
            listView.startDragAndDrop(TransferMode.MOVE).setContent(content);
            e.consume();
        });

        listView.setOnDragOver((DragEvent e) -> {
            if (context.player != null) {
                e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            }
        });

        listView.setOnDragEntered((DragEvent e) -> {
            if (context.player != null) {
                listView.setStyle("-fx-background-color: #2a3a5e;");
                e.consume();
            }
        });

        listView.setOnDragExited((DragEvent e) -> {
            listView.setStyle("");
            e.consume();
        });

        listView.setOnDragDropped((DragEvent e) -> {
            boolean success = false;
            if (context.player != null) {
                DragGame dg = gameSupplier.get();
                if (dg != null) {
                    Game game = dg.game;
                    Team targetTeam = teamSupplier.get();
                    if (targetTeam != context.sourceTeam) {
                        if (targetTeam != null) {
                            store.moveToTeam(context.player, game, targetTeam);
                        } else {
                            store.moveToFreeAgents(context.player, game);
                        }
                        dg.onChange.run();
                        success = true;
                    }
                }
            }
            e.setDropCompleted(success);
            e.consume();
        });

        listView.setOnDragDone((DragEvent e) -> {
            listView.setStyle("");
            context = new DragContext();
            e.consume();
        });
    }

    public static class DragGame {
        public final Game game;
        public final Runnable onChange;
        public DragGame(Game game, Runnable onChange) {
            this.game = game;
            this.onChange = onChange;
        }
    }

    private static class DragContext {
        Player player;
        Game sourceGame;
        Team sourceTeam;
    }

    private static class PlayerCell extends ListCell<Player> {
        private final Supplier<DragGame> gameSupplier;

        PlayerCell(Supplier<DragGame> gameSupplier) {
            this.gameSupplier = gameSupplier;
        }

        @Override
        protected void updateItem(Player p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setText(null);
                setGraphic(null);
            } else {
                String roleLabel = "Bez uloge";
                DragGame dg = gameSupplier.get();
                if (dg != null && p.getRoleId() != null) {
                    Role r = dg.game.findRole(p.getRoleId());
                    if (r != null) roleLabel = r.getName();
                }
                setText(p.getUsername() + "  [" + roleLabel + "]");
                setGraphic(null);
            }
        }
    }
}