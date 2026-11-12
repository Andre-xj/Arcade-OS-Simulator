package arcadeos;

import java.util.*;

/**
 * Global application state and User Manager for ArcadeOS.
 * Integrates presets, authentication, and leaderboard from in-suu/MiniOs.
 */
public class AppState {
    public static class UserProfile {
        public String username;
        public String password;
        public String role; // "ADMIN" or "PLAYER/GUEST"
        public int level;
        public int score;

        public UserProfile(String username, String password, String role, int level, int score) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.level = level;
            this.score = score;
        }
    }

    public static String playerName = "GUEST_USER";
    public static String playerRole = "PLAYER/GUEST";
    public static int score = 0;
    public static int credits = 99;
    public static boolean isGuest = true;
    public static int[] filesRead = new int[8]; // 0=unread, 1=read

    // Database of users from in-suu/MiniOs
    private static final Map<String, UserProfile> userDatabase = new HashMap<>();

    static {
        // Presets from in-suu/MiniOs
        addUser("INSU", "admin123", "ADMIN", 12, 98200);
        addUser("CY", "123pass", "PLAYER/GUEST", 10, 71892);
        addUser("DRE", "pass1", "PLAYER/GUEST", 11, 67899);
        addUser("PAC_ID", "guest", "PLAYER/GUEST", 8, 53180);
        addUser("PLAYER_1", "pass1", "ADMIN", 3, 12500);
        addUser("RAM_RANGER", "pass2", "PLAYER/GUEST", 2, 8900);
    }

    private static void addUser(String username, String pass, String role, int level, int score) {
        userDatabase.put(username.toUpperCase(), new UserProfile(username, pass, role, level, score));
    }

    public static String getScoreFormatted() {
        return String.format("%06d", score);
    }

    public static void addScore(int pts) {
        score += pts;
        if (!isGuest && userDatabase.containsKey(playerName.toUpperCase())) {
            UserProfile u = userDatabase.get(playerName.toUpperCase());
            u.score = Math.max(u.score, score);
        }
    }

    public static void markRead(int index) {
        if (index >= 0 && index < 8) {
            filesRead[index] = 1;
        }
    }

    public static int countFilesRead() {
        int c = 0;
        for (int v : filesRead) {
            if (v == 1) c++;
        }
        return c;
    }

    /** Authenticate against database */
    public static boolean login(String username, String password) {
        String key = username.trim().toUpperCase();
        if (userDatabase.containsKey(key)) {
            UserProfile u = userDatabase.get(key);
            if (u.password.equals(password)) {
                playerName = u.username;
                playerRole = u.role;
                score = u.score;
                isGuest = false;
                return true;
            }
        }
        return false;
    }

    /** Register new administrator / root user */
    public static String registerUser(String username, String password, String confirmPassword, String role) {
        if (username == null || username.trim().length() < 3) {
            return "ERROR: USERNAME MUST HAVE AT LEAST 3 CHARACTERS";
        }
        if (!password.equals(confirmPassword)) {
            return "ERROR: PASSWORDS DO NOT MATCH";
        }
        String key = username.trim().toUpperCase();
        if (userDatabase.containsKey(key)) {
            return "ERROR: USERNAME ALREADY EXISTS";
        }

        UserProfile newUser = new UserProfile(username.trim(), password, role, 1, 0);
        userDatabase.put(key, newUser);
        playerName = newUser.username;
        playerRole = newUser.role;
        score = 0;
        isGuest = false;
        return "SUCCESS: KERNEL INITIALIZED SUCCESSFULLY!";
    }

    /** Leaderboard sorted by score descending */
    public static List<UserProfile> getLeaderboard() {
        List<UserProfile> list = new ArrayList<>(userDatabase.values());
        // If current guest or active user is not in database, add transiently
        boolean found = false;
        for (UserProfile u : list) {
            if (u.username.equalsIgnoreCase(playerName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(new UserProfile(playerName, "", playerRole, 1, score));
        }

        list.sort((a, b) -> Integer.compare(b.score, a.score));
        return list;
    }
}
