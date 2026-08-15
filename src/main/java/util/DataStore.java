package util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Game;
import model.Player;
import model.Role;
import model.Team;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        load();
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

    // Brisanje tima — igraci se uvek vracaju u pool (konzistentnost stanja).
    public void removeTeam(Game game, Team team) {
        ObservableList<Player> pool = getFreeAgents(game);
        for (Player p : new ArrayList<>(team.getPlayers())) {
            team.removePlayer(p);
            pool.add(p);
        }
        game.removeTeam(team);
    }

    // Atomski premesta igraca iz pool-a ili starog tima u novi tim.
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

    // Vraca igraca iz tima u pool slobodnih igraca.
    public void moveToFreeAgents(Player player, Game game) {
        if (player.getTeam() != null) {
            player.getTeam().getPlayers().remove(player);
            player.setTeam(null);
        } else {
            return;
        }
        getFreeAgents(game).add(player);
    }

    // Serijalizacija stanja u JSON preko DTO sloja — perzistencija je razdvojena od domenskih klasa.
    public void save() {
        try {
            PersistedData data = new PersistedData();
            for (Game g : games) {
                GameDTO gd = new GameDTO(g.getId(), g.getName());
                for (Role r : g.getRoles()) {
                    gd.roles.add(new RoleDTO(r.getId(), r.getName()));
                }
                gd.freeAgents = new ArrayList<>();
                for (Player p : getFreeAgents(g)) {
                    gd.freeAgents.add(new PlayerDTO(p.getId(), p.getUsername(), p.getRoleId()));
                }
                for (Team t : g.getTeams()) {
                    TeamDTO td = new TeamDTO(t.getId(), t.getName());
                    for (Player p : t.getPlayers()) {
                        td.players.add(new PlayerDTO(p.getId(), p.getUsername(), p.getRoleId()));
                    }
                    gd.teams.add(td);
                }
                data.games.add(gd);
            }
            mapper.writeValue(dataPath.toFile(), data);
        } catch (Exception e) {
            System.err.println("Greska pri cuvanju podataka: " + e.getMessage());
        }
    }

    private void load() {
        File f = dataPath.toFile();
        if (!f.exists()) return;
        try {
            PersistedData data = mapper.readValue(f, PersistedData.class);
            if (data == null || data.games == null) return;
            for (GameDTO gd : data.games) {
                Game g = new Game(gd.id, gd.name);
                if (gd.roles != null) {
                    for (RoleDTO rd : gd.roles) g.addRole(new Role(rd.id, rd.name, g.getId()));
                }
                ObservableList<Player> pool = FXCollections.observableArrayList();
                if (gd.freeAgents != null) {
                    for (PlayerDTO pd : gd.freeAgents) {
                        pool.add(new Player(pd.id, pd.username, pd.roleId, g.getId()));
                    }
                }
                freeAgentsByGame.put(g.getId(), pool);
                if (gd.teams != null) {
                    for (TeamDTO td : gd.teams) {
                        Team t = new Team(td.id, td.name, g.getId());
                        if (td.players != null) {
                            for (PlayerDTO pd : td.players) {
                                Player p = new Player(pd.id, pd.username, pd.roleId, g.getId());
                                t.addPlayer(p);
                            }
                        }
                        g.addTeam(t);
                    }
                }
                games.add(g);
            }
        } catch (Exception e) {
            System.err.println("Greska pri ucitavanju podataka: " + e.getMessage());
        }
    }

    // DTO sloj — razdvaja JSON reprezentaciju od domenskih klasa (model ostaje perzistencijski agnosticican).
    private static class PersistedData {
        @JsonProperty("games")
        public List<GameDTO> games = new ArrayList<>();
    }

    private static class GameDTO {
        @JsonProperty("id") public String id;
        @JsonProperty("name") public String name;
        @JsonProperty("roles") public List<RoleDTO> roles = new ArrayList<>();
        @JsonProperty("freeAgents") public List<PlayerDTO> freeAgents = new ArrayList<>();
        @JsonProperty("teams") public List<TeamDTO> teams = new ArrayList<>();

        GameDTO() {}
        GameDTO(String id, String name) { this.id = id; this.name = name; }
    }

    private static class RoleDTO {
        @JsonProperty("id") public String id;
        @JsonProperty("name") public String name;
        RoleDTO() {}
        RoleDTO(String id, String name) { this.id = id; this.name = name; }
    }

    private static class PlayerDTO {
        @JsonProperty("id") public String id;
        @JsonProperty("username") public String username;
        @JsonProperty("roleId") public String roleId;
        PlayerDTO() {}
        PlayerDTO(String id, String username, String roleId) {
            this.id = id; this.username = username; this.roleId = roleId;
        }
    }

    private static class TeamDTO {
        @JsonProperty("id") public String id;
        @JsonProperty("name") public String name;
        @JsonProperty("players") public List<PlayerDTO> players = new ArrayList<>();
        TeamDTO() {}
        TeamDTO(String id, String name) { this.id = id; this.name = name; }
    }
}