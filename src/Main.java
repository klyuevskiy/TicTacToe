import java.sql.*;

public class Main {
    public static void main(String[] args) throws SQLException {
        Connection connection = DriverManager.getConnection(
                args[0], args[1], args[2]
        );

        Player player = new Player(connection);
        player.start();

        connection.close();
    }
}