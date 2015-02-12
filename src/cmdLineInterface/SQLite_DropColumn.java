package cmdLineInterface;

import java.sql.*;
import java.util.Scanner;
import java.util.ArrayList;

public class SQLite_DropColumn {

	Connection c = null;
	Statement stmt = null;
	String table = null; // name of table that contains dropped column
	String dropcol = null; // column dropped
	String indexName = null;
	int indexFlag = 0; 


	public boolean checkIfPrimaryKey(String tableName, String columnName) throws Exception {
	    //if pk equals 999 at end of the program then columnName was not found in table (throw exception?)
	    int pk = 999;

	    Class.forName("org.sqlite.JDBC");
	    c = DriverManager.getConnection("jdbc:sqlite:test.db");

	    stmt = c.createStatement();
	    ResultSet rs = stmt.executeQuery( "PRAGMA table_info(" + tableName + ");" );

	    while ( rs.next() ) {
	        if (columnName.equals(rs.getString("name"))) {
	           pk = rs.getInt("pk");
	        }
	    }

	    rs.close();

	    //alert user if column is primary key
	    return pk==1 ? true : false;
    }

    public int checkIndex(String currentColumn, String tableName) {
		ArrayList<String> index = new ArrayList<String>();
		try {
			ResultSet ri = stmt.executeQuery("PRAGMA index_list(" + tableName + ");");
			int count = 0;
			while (ri.next()) {
				count++;   //count of rs is required.  Resultset is forward only.
			}
			ri.close();
		    ResultSet r2 = stmt.executeQuery("PRAGMA index_list(" + tableName + ");");
			for (int i = 0; i < count; i++) {   //store pragma index list in arraylist to avoid inconsistent state error
				String indexInfo = r2.getString("name");
				index.add(indexInfo);
			//	System.out.println("Added to list: "+Index.get(i));
				if (r2.next()) {
					r2.next();
				}
			} 
			r2.close();
			for (int i = 0; i < index.size(); i++) {
					// String indexName = index.get(i);
					indexName = index.get(i);
					ResultSet r = stmt.executeQuery("PRAGMA index_info(" + indexName + ");");
					String indexColumnName = r.getString("name");
					if(indexColumnName.equals(currentColumn)) {  //if index found
						indexFlag = 1;
						System.out.println("Index detected on given column: " + currentColumn);
						return 1;
					}
			}  
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}


    public void dropColumn() {
		try {
			ArrayList<String> primarykeys = new ArrayList<String>();
			Scanner userinput = new Scanner(System.in);

			// Establish connection with database
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			// c.setAutoCommit(false);
			System.out.println("Opened database successfully\n");

			// Prints list of tables in database
			stmt = c.createStatement();
			ResultSet tblList = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
			System.out.println("Tables in database: ");
			while(tblList.next()) {
				String name = tblList.getString("name");
				System.out.println(name);
			}
			tblList.close();

			// User inputs a table 
			System.out.print("\nChoose a table: ");
			table = userinput.next();

			// Prints columns in selected table 
			ArrayList<String> colList = new ArrayList<String>();
			ArrayList<String> datatypes = new ArrayList<String>();
			ResultSet columns = stmt.executeQuery("PRAGMA table_info(" + table + ");");
			System.out.println("\nColumns in " + table + ": ");
			while(columns.next()) {
				String colname = columns.getString("name");
				String data = columns.getString("type");
				if (checkIfPrimaryKey(table, colname)) {
					primarykeys.add(colname);
				}
				colList.add(colname);
				datatypes.add(data);
				System.out.println(colname + " " + data + " " + columns.getString("pk"));
			}
			columns.close();

			
			// Checks if all attributes are primary keys or not.
			if (colList.size() == primarykeys.size()) {
				System.out.println("Sorry, all attributes remaining are primary keys. You cannot drop primary keys.");
			}
			else {
				// User input which column to drop
				System.out.print("\nChoose a column to drop: ");
				// String dropcol = userinput.next();
				dropcol = userinput.next();


				// Checks if column exists. Case-sensitive. 
				while (!colList.contains(dropcol)) {
					System.out.println("Sorry, this column does exist. Try again.");
					System.out.print("Choose another column: ");
					dropcol = userinput.next();
				}

				// Checks if chosen column is a primary key
				while (primarykeys.contains(dropcol)) {
					System.out.println("Sorry, you can't drop a primary key!");
					System.out.print("Choose another column: ");
					dropcol = userinput.next();
				}

				// Add columns to keep in new array
				ArrayList<String> newCol = new ArrayList<String>();
				ArrayList<String> newType = new ArrayList<String>();
				for (int i = 0; i < colList.size(); i++) {
					if (!(colList.get(i)).toString().equals(dropcol)) {
						newCol.add((colList.get(i)).toString());
						newType.add((datatypes.get(i)).toString());
						// System.out.println("Keeping: " + (colList.get(i)).toString());
					}
				}

				// Create string of columns to be inserted in temporary table
				// Includes primary key and data types
				String selectCols = ""; // string of columns used for SELECT
				String createtbl = ""; // string used for CREATE TABLEs 
				for (int i = 0; i < newCol.size(); i++) {
					String namedata = newCol.get(i).toString() + " " + newType.get(i).toString();
					if (i < newCol.size() - 1) {
						if (primarykeys.contains(newCol.get(i).toString())) {
							createtbl += namedata + " PRIMARY KEY, ";
						}
						else {
							createtbl += namedata + ", ";
						}
						selectCols += newCol.get(i).toString() + ",";
					}
					else {
						if (primarykeys.contains(newCol.get(i).toString())) {
							createtbl += namedata + " PRIMARY KEY";
						}
						else {
							createtbl += namedata;
						}
						selectCols += newCol.get(i).toString();
					}
				}
				// System.out.println(createtbl);
				// System.out.println(selectCols);

				// Check for index and drop index 
				int check = checkIndex(dropcol, table);
				if (check == 1) {
					System.out.println("Index found: " + indexName);
					stmt.executeUpdate("DROP INDEX " + indexName + ";");
				}

				// Queries to drop column
				String sql = "CREATE TEMPORARY TABLE t_backup(" + selectCols + ");";
				stmt.executeUpdate(sql);
				sql = "INSERT INTO t_backup SELECT " + selectCols + " FROM " + table + ";";
				stmt.executeUpdate(sql);
				sql = "DROP TABLE " + table + ";";
				stmt.executeUpdate(sql);
				sql = "CREATE TABLE " + table + "(" + createtbl + ");";
				stmt.executeUpdate(sql);
				sql = "INSERT INTO " + table + " SELECT " + selectCols + " FROM t_backup;";
				stmt.executeUpdate(sql);
				sql = "DROP TABLE t_backup;";
				stmt.executeUpdate(sql);
				// c.commit();
			}
			
			userinput.close();
			stmt.close();
			c.close();
		}
		catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Operation done successfully");
    }

}