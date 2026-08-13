package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class Role {

    private final String id;
    private final StringProperty name = new SimpleStringProperty();
    private final String gameId;

    public Role(String name, String gameId) {
        this.id = UUID.randomUUID().toString();
        this.gameId = gameId;
        setName(name);
    }

    public Role(String id, String name, String gameId) {
        this.id = id;
        this.gameId = gameId;
        setName(name);
    }

    public String getId() { return id; }

    public String getGameId() { return gameId; }

    public String getName() { return name.get(); }

    public void setName(String name) { this.name.set(name); }

    public StringProperty nameProperty() { return name; }

    @Override
    public String toString() { return getName(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}