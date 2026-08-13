package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class Game {

    private final String id;
    private final StringProperty name = new SimpleStringProperty();

    // TODO: definisati ObservableList<Role> roles
    // TODO: definisati ObservableList<Team> teams

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

    // TODO: getRoles(), getTeams()
    // TODO: findRole(roleId)
    // TODO: findTeam(teamId) nznm
    // TODO: addRole(role), removeRole(role)
    // TODO: addTeam(team), removeTeam(team) ??

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