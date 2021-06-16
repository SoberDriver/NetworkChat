import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class BaseAuthService implements AuthService{
    private static Connection connection;
    private static Statement statement;

    private static class Entry {
        private String login;
        private String pass;
        private String nick;


        public Entry(String login, String pass, String nick) {
            this.login = login;
            this.pass = pass;
            this.nick = nick;
            connection();
            int hash = pass.hashCode();
            String sql0 = "CREATE TABLE IF NOT EXISTS Clients\n" +
                    "(\n" +
                    "  login TEXT NOT NULL,\n" +
                    "  password TEXT NOT NULL,\n" +
                    "  nickname TEXT NOT NULL\n" +
                    ");";
            String sql1 = String.format("INSERT INTO Clients (login, password, nickname) VALUES ('%s', '%d', '%s')", login, hash, nick);
            try {
                int rs = statement.executeUpdate(sql0);
                rs = statement.executeUpdate(sql1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            disconnect();
        }

    }

    private List<Entry> entries;

    static void connection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:mainDataBase.db");
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public BaseAuthService() {
        entries = new ArrayList<>();
        entries.add(new Entry("login1", "pass1", "nick1"));
        entries.add(new Entry("login2", "pass2", "nick2"));
        entries.add(new Entry("login3", "pass3", "nick3"));
        entries.add(new Entry("login4", "pass4", "nick4"));
    }

    @Override
    public void start() {
        System.out.println("Сервис аутентификации запущен.");
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        for (Entry o: entries) {
            if (o.login.equals(login) && o.pass.equals(pass)) {
                return o.nick;
            }
        }
        return null;
    }

    @Override
    public void stop() {
        System.out.println("Сервер аутентификации остановлен.");
    }

    @Override
    public String changeNickname (String oldNick, String newNick) {
        connection();
        try {
            statement.executeUpdate("UPDATE Clients SET nickname = \"" + newNick +"\" WHERE nickname = \"" + oldNick + "\"");
            return newNick;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка обновления ника");
        } finally {
            disconnect();
        }
        return oldNick;
    }
}
