package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.UUID;

public class Game {

    private final String id;
    private final StringProperty name = new SimpleStringProperty();
    private final ObservableList<Role> roles = FXCollections.observableArrayList();
    private final ObservableList<Team> teams = FXCollections.observableArrayList();

    public Game(String name) {
        this.id = UUID.randomUUID().toString();
        setName(name);
    }

    public Game(String id, String name) {
        this.id = id;
        setName(name);
    }

    public String getId() { return id; }

    public String getName() { return name.get(); }

    public void setName(String name) { this.name.set(name); }

    public StringProperty nameProperty() { return name; }

    public ObservableList<Role> getRoles() { return roles; }

    public ObservableList<Team> getTeams() { return teams; }

    public Role findRole(String roleId) {
        if (roleId == null) return null;
        return roles.stream()
                .filter(r -> r != null && roleId.equals(r.getId()))
                .findFirst()
                .orElse(null);
    }

    public Team findTeam(String teamId) {
        if (teamId == null) return null;
        return teams.stream().filter(t -> t.getId().equals(teamId)).findFirst().orElse(null);
    }

    public void addRole(Role role) { roles.add(role); }

    public void removeRole(Role role) {
        roles.remove(role);
        for (Team t : teams) {
            t.getPlayers().forEach(p -> {
                if (role.getId().equals(p.getRoleId())) p.setRoleId(null);
            });
        }
    }

    public void addTeam(Team team) { teams.add(team); }

    public void removeTeam(Team team) { teams.remove(team); }

    @Override
    public String toString() { return getName(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Game other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}