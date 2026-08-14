package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class Player {

    private final String id;
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty roleId = new SimpleStringProperty();
    private final String gameId;

    private Team team;

    public Player(String username, String roleId, String gameId) {
        this.id = UUID.randomUUID().toString();
        this.gameId = gameId;
        setUsername(username);
        setRoleId(roleId);
    }

    public Player(String id, String username, String roleId, String gameId) {
        this.id = id;
        this.gameId = gameId;
        setUsername(username);
        setRoleId(roleId);
    }

    public String getId() { return id; }

    public String getGameId() { return gameId; }

    public String getUsername() { return username.get(); }

    public void setUsername(String username) { this.username.set(username); }

    public StringProperty usernameProperty() { return username; }

    public String getRoleId() { return roleId.get(); }

    public void setRoleId(String roleId) { this.roleId.set(roleId); }

    public StringProperty roleIdProperty() { return roleId; }

    public Team getTeam() { return team; }

    public void setTeam(Team team) { this.team = team; }

    public boolean isFreeAgent() { return team == null; }

    public String displayLabel() {
        return getUsername();
    }

    @Override
    public String toString() { return getUsername(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
