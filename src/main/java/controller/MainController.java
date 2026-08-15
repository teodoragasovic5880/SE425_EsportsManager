package controller;

import model.Game;
import model.Player;
import model.Role;
import model.Team;
import util.DataStore;
import util.DragDropHelper;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {

    @FXML private ListView<Game> gamesList;
    @FXML private ListView<Player> poolView;
    @FXML private VBox teamsContainer;
    @FXML private Label selectedGameLabel;
    @FXML private Label rolesLabel;
    @FXML private Label statusLabel;
    @FXML private Label gameCountLabel;
    @FXML private Label poolCountLabel;

    private DataStore store;
    private DragDropHelper dd;
    private Game selectedGame;
    private final List<ListView<Player>> teamViews = new ArrayList<>();

    @FXML
    public void initialize() {
        store = new DataStore();
        dd = new DragDropHelper(store);

        gamesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Game g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) setText(null);
                else setText(g.getName() + "  (" + g.getTeams().size() + " timova)");
            }
        });

        gamesList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> selectGame(sel));

        gamesList.setItems(store.getGames());
        updateGameCount();

        store.getGames().addListener((ListChangeListener<Game>) c -> updateGameCount());

        if (!store.getGames().isEmpty()) {
            gamesList.getSelectionModel().select(0);
        }
        setStatus("Spreman.");
    }

    private void selectGame(Game game) {
        selectedGame = game;
        refreshGameView();
    }

    private void refreshGameView() {
        teamViews.clear();
        teamsContainer.getChildren().clear();

        if (selectedGame == null) {
            selectedGameLabel.setText("Nema selektovane igre");
            rolesLabel.setText("");
            poolView.setItems(null);
            poolCountLabel.setText("");
            teamsContainer.getChildren().add(new Label("Selektuj igru sa leve strane.") {{
                setStyle("-fx-text-fill: #8899aa; -fx-padding: 20;");
            }});
            return;
        }

        selectedGameLabel.setText(selectedGame.getName());

        StringBuilder roles = new StringBuilder();
        if (selectedGame.getRoles().isEmpty()) {
            roles.append("Nema definisanih uloga");
        } else {
            roles.append("Uloge: ");
            for (Role r : selectedGame.getRoles()) {
                if (r != null) roles.append(r.getName()).append(", ");
            }
            if (roles.length() > 7) roles.setLength(roles.length() - 2);
        }
        rolesLabel.setText(roles.toString());

        poolView.setItems(store.getFreeAgents(selectedGame));
        poolCountLabel.setText(String.valueOf(store.getFreeAgents(selectedGame).size()));
        dd.configure(poolView, this::currentDragGame, () -> null);

        if (selectedGame.getTeams().isEmpty()) {
            teamsContainer.getChildren().add(new Label(
                    "Trenutno nema timova. Klikni \u201eNovi tim\u201c.") {{
                setStyle("-fx-text-fill: #8899aa; -fx-padding: 20;");
            }});
        } else {
            for (Team t : selectedGame.getTeams()) {
                teamsContainer.getChildren().add(buildTeamCard(t));
            }
        }
    }

    private DragDropHelper.DragGame currentDragGame() {
        if (selectedGame == null) return null;
        return new DragDropHelper.DragGame(selectedGame, this::refreshGameView);
    }

    private VBox buildTeamCard(Team team) {
        ListView<Player> teamView = new ListView<>();
        teamView.setItems(team.getPlayers());
        teamView.setPrefHeight(180);
        teamView.getStyleClass().add("player-list");
        final Team myTeam = team;
        dd.configure(teamView, this::currentDragGame, () -> myTeam);
        teamViews.add(teamView);

        Label teamName = new Label();
        teamName.textProperty().bind(team.nameProperty());
        teamName.getStyleClass().add("team-name");

        Label roster = new Label();
        roster.getStyleClass().add("roster-label");

        Button deleteBtn = new Button("x");
        deleteBtn.getStyleClass().add("delete-btn");
        deleteBtn.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Obrisati tim \"" + team.getName() + "\"? Igraci ce biti vraceni u pool.",
                    ButtonType.YES, ButtonType.NO);
            a.setHeaderText(null);
            Optional<ButtonType> r = a.showAndWait();
            if (r.isPresent() && r.get() == ButtonType.YES) {
                store.removeTeam(selectedGame, team);
                store.save();
                refreshGameView();
                setStatus("Tim obrisan.");
            }
        });

        HBox header = new HBox(10, teamName, new Region(), roster, deleteBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        VBox card = new VBox(6, header, teamView);
        card.getStyleClass().add("team-card");
        VBox.setVgrow(card, Priority.NEVER);
        VBox.setMargin(card, new Insets(0));

        Runnable updateRoster = () -> {
            roster.setText(team.getPlayers().size() + " igraca / " + countRoles(team) + " uloga");
        };
        team.getPlayers().addListener((ListChangeListener<Player>) c -> updateRoster.run());
        updateRoster.run();

        return card;
    }

    private String countRoles(Team team) {
        return String.valueOf(selectedGame.getRoles().size());
    }

    @FXML
    private void onAddGame() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nova igra");
        d.setHeaderText(null);
        d.setContentText("Naziv igre:");
        Optional<String> r = d.showAndWait();
        if (r.isPresent() && !r.get().isBlank()) {
            Game g = store.addGame(r.get().trim());
            store.save();
            gamesList.getSelectionModel().select(g);
            setStatus("Dodata igra: " + g.getName());
        }
    }

    @FXML
    private void onAddRole() {
        Game g = requireSelectedGame();
        if (g == null) return;

        Dialog<Role> d = new Dialog<>();
        d.setTitle("Nova uloga");
        d.setHeaderText("Definisi ulogu za igru: " + g.getName());
        ButtonType ok = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("npr. Mid, Support, Entry Fragger");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Naziv uloge:"), 0, 0);
        grid.add(nameField, 1, 0);
        d.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        d.setResultConverter(btn -> btn == ok && !nameField.getText().isBlank()
                ? new Role(nameField.getText().trim(), g.getId())
                : null);
        Optional<Role> r = d.showAndWait();
        if (r.isPresent()) {
            store.addRole(g, r.get().getName());
            store.save();
            refreshGameView();
            setStatus("Dodata uloga: " + r.get().getName());
        }
    }

    @FXML
    private void onAddPlayer() {
        Game g = requireSelectedGame();
        if (g == null) return;

        if (g.getRoles().isEmpty()) {
            info("Morate prvo dodati barem jednu ulogu za ovu igru.");
            return;
        }

        Dialog<Player> d = new Dialog<>();
        d.setTitle("Novi igrac");
        d.setHeaderText("Dodaj profil igraca za igru: " + g.getName());
        ButtonType ok = new ButtonType("Dodaj", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Korisnicko ime");
        ComboBox<Role> roleCombo = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(g.getRoles()));
        roleCombo.getItems().add(0, null);
        roleCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Role r) { return r == null ? "Bez uloge" : r.getName(); }
            @Override public Role fromString(String s) { return null; }
        });

        GridPane grid2 = new GridPane();
        grid2.setHgap(10);
        grid2.setVgap(10);
        grid2.setPadding(new Insets(20));
        grid2.add(new Label("Korisnicko ime:"), 0, 0);
        grid2.add(usernameField, 1, 0);
        grid2.add(new Label("Preferirana uloga:"), 0, 1);
        grid2.add(roleCombo, 1, 1);
        d.getDialogPane().setContent(grid2);
        Platform.runLater(usernameField::requestFocus);

        d.setResultConverter(btn -> {
            if (btn == ok && usernameField.getText() != null && !usernameField.getText().isBlank()) {
                Role selected = roleCombo.getSelectionModel().getSelectedItem();
                return new Player(usernameField.getText().trim(),
                        selected != null ? selected.getId() : null, g.getId());
            }
            return null;
        });

        Optional<Player> r = d.showAndWait();
        if (r.isPresent()) {
            Player p = r.get();
            store.getFreeAgents(g).add(p);
            store.save();
            refreshGameView();
            setStatus("Dodat igrac: " + p.getUsername());
        }
    }

    @FXML
    private void onAddTeam() {
        Game g = requireSelectedGame();
        if (g == null) return;

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Novi tim");
        d.setHeaderText(null);
        d.setContentText("Naziv tima:");
        Optional<String> r = d.showAndWait();
        if (r.isPresent() && !r.get().isBlank()) {
            store.addTeam(g, r.get().trim());
            store.save();
            refreshGameView();
            setStatus("Dodat tim: " + r.get().trim());
        }
    }

    @FXML
    private void onSave() {
        store.save();
        setStatus("Podaci sacuvani.");
    }

    private Game requireSelectedGame() {
        if (selectedGame == null) {
            info("Prvo selektuj igru sa leve strane.");
            return null;
        }
        return selectedGame;
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void updateGameCount() {
        gameCountLabel.setText("Ukupno igara: " + store.getGames().size());
    }
}