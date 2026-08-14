package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;

public class Team {

    private final String id;
    private final StringProperty name = new SimpleStringProperty();
    private final String gameId;
    private final ObservableList<Player> players = FXCollections.observableArrayList();

    public Team(String name, String gameId) {
        this.id = UUID.randomUUID().toString();
        this.gameId = gameId;
        setName(name);
    }

    public Team(String id, String name, String gameId) {
        this.id = id;
        this.gameId = gameId;
        setName(name);
    }

    public String getId() { return id; }

    public String getGameId() { return gameId; }

    public String getName() { return name.get(); }

    public void setName(String name) { this.name.set(name); }

    public StringProperty nameProperty() { return name; }

    public ObservableList<Player> getPlayers() { return players; }

    public void addPlayer(Player player) {
        if (player == null || players.contains(player)) return;
        if (player.getTeam() != null) {
            player.getTeam().getPlayers().remove(player);
        }
        player.setTeam(this);
        players.add(player);
    }

    public void removePlayer(Player player) {
        if (players.remove(player)) {
            player.setTeam(null);
        }
    }

    @Override
    public String toString() { return getName(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}