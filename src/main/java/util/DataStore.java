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
import java.util.ArrayList;
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

        // TODO: load() — deserijalizacija stanja iz JSON-a (vidi kasniji DTO refactor commit)
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

    public void removeTeam(Game game, Team team) {
        ObservableList<Player> pool = getFreeAgents(game);
        for (Player p : new ArrayList<>(team.getPlayers())) {
            team.removePlayer(p);
            pool.add(p);
        }
        game.removeTeam(team);
    }

    public void moveToTeam(Player player, Game game, Team targetTeam) {
        if (targetTeam != null && targetTeam.equals(player.getTeam())) return;

        if (player.getTeam() != null) {
            player.getTeam().getPlayers().remove(player);
            player.setTeam(null);
        } else {
            getFreeAgents(game).remove(player);
        }
        targetTeam.addPlayer(player);
    }

    public void moveToFreeAgents(Player player, Game game) {
        if (player.getTeam() != null) {
            player.getTeam().getPlayers().remove(player);
            player.setTeam(null);
        } else {
            return;
        }
        getFreeAgents(game).add(player);
    }

    // TODO: save() — serijalizacija stanja u data.json sa DTO slojem (razdvojenost perzistencije i modela)
    // TODO: load() — deserijalizacija stanja iz data.json
}