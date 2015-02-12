package cmdLineInterface;

import java.sql.*;

public class SQLiteAlter {
    static Connection c = null;
    static Statement stmt = null;

    public static void executeAddColumn(String tableName, String colName, String dataType, int length) throws Exception{
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:test.db");

        stmt = c.createStatement();
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + colName
            + " " + dataType + "(" + length +")";
        stmt.executeUpdate(sql);
        stmt.close();
        c.close();
        System.out.println("Operation done successfully");
    }

    public static void renameTable(String tableName, String newTableName) throws Exception{
        // Establish connection with database
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:test.db");

        stmt = c.createStatement();

        String sql = "ALTER TABLE " + tableName + " RENAME TO " + newTableName + ";";
        stmt.executeUpdate(sql);

        stmt.close();
        c.close();
        System.out.println("Operation done successfully");
    }
}
