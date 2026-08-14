package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Game;
import model.Player;
import model.Role;
import model.Team;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DataStore {

    private static final String APP_DIR = ".esport-team-manager";
    private static final String DATA_FILE = "data.json";

    private final ObservableList<Game> games = FXCollections.observableArrayList();
    private final Map<String, ObservableList<Player>> freeAgentsByGame = new HashMap<>();

    private final ObjectMapper mapper;
    private final Path dataPath;

    public DataStore() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Path dir = Path.of(System.getProperty("user.home"), APP_DIR);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("Nije moguce kreirati direktorijum: " + e.getMessage());
        }
        this.dataPath = dir.resolve(DATA_FILE);

        // TODO: load() — deserijalizacija stanja iz JSON-a
    }

    public ObservableList<Game> getGames() {
        return games;
    }

    public ObservableList<Player> getFreeAgents(Game game) {
        return freeAgentsByGame.computeIfAbsent(
                game.getId(),
                k -> FXCollections.observableArrayList()
        );
    }

    public Game addGame(String name) {
        Game g = new Game(name);
        games.add(g);
        freeAgentsByGame.put(g.getId(), FXCollections.observableArrayList());
        return g;
    }

    public void removeGame(Game game) {
        games.remove(game);
        freeAgentsByGame.remove(game.getId());
    }

    public Role addRole(Game game, String roleName) {
        Role role = new Role(roleName, game.getId());
        game.addRole(role);
        return role;
    }

    public void removeRole(Game game, Role role) {
        game.removeRole(role);
    }

    public Player addPlayer(Game game, String username, Role role) {
        Player p = new Player(username, role != null ? role.getId() : null, game.getId());
        getFreeAgents(game).add(p);
        return p;
    }

    public void removePlayer(Game game, Player player) {
        if (player.getTeam() != null) {
            player.getTeam().removePlayer(player);
        } else {
            getFreeAgents(game).remove(player);
        }
    }

    public Team addTeam(Game game, String teamName) {
        Team t = new Team(teamName, game.getId());
        game.addTeam(t);
        return t;
    }

    // TODO: removeTeam
    // TODO: moveToTeam(player, game, targetTeam)
    // TODO: moveToFreeAgents(player, game) premštanje igrača iz tima nazad u pool

    // TODO: save() — serijalizacija  u data.json sa DTO slojem
    // TODO: load() — deserijalizacija iz data.json
}