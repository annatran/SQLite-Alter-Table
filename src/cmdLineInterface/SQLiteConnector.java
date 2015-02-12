package cmdLineInterface;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Scanner;
import java.util.ArrayList;


public class SQLiteConnector {
	public SQLiteConnector() {
		Connection c = null;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:test.db");
	      c.close();
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    System.out.println("Opened database successfully");

	}

	public static void main(String[] args) throws IOException {
		SQLiteConnector sql = new SQLiteConnector();
		sql.showMenu();

	}

        public static void printTables() throws Exception {
            Connection c = null;
            Statement stmt = null;

            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");

            stmt = c.createStatement();

            // Prints list of tables in database
            ResultSet tblList = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
            System.out.println("\nTables in database: ");
            while(tblList.next()) {
                    String name = tblList.getString("name");
                    System.out.println(name);
            }
            System.out.println();

            tblList.close();
            stmt.close();
            c.close();
        }

        public static ArrayList<String> printColumns(String table) throws Exception {
            Connection c = null;
            Statement stmt = null;

            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:test.db");

            stmt = c.createStatement();

            // Prints columns in selected table
            ArrayList<String> colList = new ArrayList<String>();
            ArrayList<String> datatypes = new ArrayList<String>();
            ResultSet columns = stmt.executeQuery("PRAGMA table_info(" + table + ");");
            System.out.println("\nColumns in " + table + ": ");
            while(columns.next()) {
                    String colname = columns.getString("name");
                    String data = columns.getString("type");
                    colList.add(colname);
                    datatypes.add(data);
                    System.out.println(colname + " " + data + " " + columns.getString("pk"));
            }
            System.out.println();

            columns.close();
            stmt.close();
            c.close();
            return colList;
        }

	public void showMenu() throws IOException {
		Scanner s = new Scanner(System.in);
		BufferedReader b = new BufferedReader(new InputStreamReader(System.in));
		int choice;
	    do{
	    	System.out.println("\nMenu");
		    System.out.println("1. Rename Column");
		    System.out.println("2. Drop Column");
		    System.out.println("3. Change Column Data Type");
		    System.out.println("4. Add Column");
		    System.out.println("5. Rename Table");
		    System.out.println("6. Display Table Metadata");
		    System.out.println("7. Exit");
		    System.out.println("Select Operation: ");
		    choice = s.nextInt();

                try {
                    switch(choice) {
                            case 1:
                                    SQLite_RenameColumn rc = new SQLite_RenameColumn();

                                    printTables();

                                    System.out.println("Enter name of table to alter: ");
                                    String tableName = b.readLine();

                                    printColumns(tableName);

                                    System.out.println("Enter old column name: ");
                                    String oldColumn = b.readLine();
                                    System.out.println("Enter new column name: ");
                                    String newColumn = b.readLine();
                                    rc.executeRename(tableName,oldColumn,newColumn);
                                    break;

                            case 2:
                                    SQLite_DropColumn dc = new SQLite_DropColumn();
                                    dc.dropColumn();
                                    break;

                            case 3:
                                    System.out.println("Supported conversions are: ");
                                    System.out.println("1. INT to String");
                                    System.out.println("2. FLOAT to String");

                                    printTables();

                                    System.out.println("Enter name of table to alter: ");
                                    tableName = b.readLine();

                                    printColumns(tableName);

                                    System.out.println("Enter name of column: ");
                                    oldColumn = b.readLine();
                                    SQLite_ChangeColDatatype cd = new SQLite_ChangeColDatatype();
                                    cd.changeColumnDataTypeIS(tableName,oldColumn,oldColumn);
                                    break;

                            case 4:
                                    printTables();

                                    System.out.println("Enter name of table to alter: ");
                                    tableName = b.readLine();

                                    ArrayList<String> colList = printColumns(tableName);

                                    System.out.println("Enter new column name: ");
                                    newColumn = b.readLine();
                                    while (colList.contains(newColumn)) {
                                        System.out.println("That column already exists. Please enter new column name: ");
                                        newColumn = b.readLine();
                                    }

                                    System.out.println("Enter new column data type: ");
                                    String newDataType = b.readLine();
                                    System.out.println("Enter new column data type length: ");
                                    int newDataTypeLength = Integer.parseInt(b.readLine());
                                    
                                    SQLiteAlter.executeAddColumn(tableName,newColumn,newDataType,newDataTypeLength);
                                    break;
                                    
                            case 5:
                                    printTables();

                                    System.out.println("Enter name of the table to rename: ");
                                    String oldTableName = b.readLine();
                                    System.out.println("Enter new name for table: ");
                                    String newTableName = b.readLine();
                                    SQLiteAlter.renameTable(oldTableName,newTableName);
                                    break;
                                    
                            case 6:
                            		printTables();
                            		System.out.println("Select table to display: ");
                            		String Table = b.readLine();
                            		printColumns(Table);
                            		break;
                                    
                            case 7:
                                    System.exit(0);
                    }
                } catch ( Exception e ) {
                    System.err.println( e.getClass().getName() + ": " + e.getMessage() );
                    System.exit(0);
                }
	    } while(choice!=7);
	    s.close();
	}
}
