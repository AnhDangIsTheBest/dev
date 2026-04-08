import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String URL = "jdbc:mysql://auction-db-auction-system.a.aivencloud.com:23104/defaultdb?sslMode=REQUIRED";   
    private static final String USER = "avnamin";
    private static final String PASSWORD = "AVNS_H8nWIUDmI3M0krkJZMz";

    public static Connection getConnection() throws Exception{
        return DriverManager.getConnection(URL,USER,PASSWORD);
    }
}
