package cmdLineInterface;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SQLite_ChangeColDatatype {
	String TableName = null;
	String newColumn = null;
	String prevColName = null;
	String sql = null;
	Statement statement = null;
	String oldIndexName = null;  //globals for restoring index later
	String oldIndexColumnName = null;
	Connection c = null;
	int i=1, indexFlag=0;
	
		public void changeColumnDataTypeIS(String Table, String OldColumn, String Column) throws Exception {
			prevColName = OldColumn;
			ArrayList<String> ColumnNames = new ArrayList<String>();
			ArrayList<String> ColumnTypes = new ArrayList<String>();
			ArrayList<String> cacheIndexColumns = new ArrayList<String>();
			ArrayList<String> cacheIndexNames = new ArrayList<String>();
			
			sql = "ALTER TABLE "+Table+" RENAME to tempTable;";
			try {
					c = DriverManager.getConnection("jdbc:sqlite:test.db");
					statement = c.createStatement();
					statement.executeUpdate(sql);
					ResultSet rs = statement.executeQuery("select * from tempTable;");
					ResultSetMetaData metaData = rs.getMetaData();
			
					int count = metaData.getColumnCount();
					for(int i=0; i < count; i++) {
						ColumnNames.add(metaData.getColumnName(i+1));
						ColumnTypes.add(metaData.getColumnTypeName(i+1));
						//System.out.println(ColumnNames.get(i)+" "+ColumnTypes.get(i));
					}
					rs.close();
					
					if(checkIfPrimaryKey("tempTable",ColumnNames.get(0))){
						if(ColumnNames.get(0).equals(prevColName)) {
							System.out.println("First column is the Primary Key. Cannot alter PK.");
						}
						sql = "CREATE TABLE "+Table+" ("+ColumnNames.get(0)+" "+ColumnTypes.get(0)+" primary key);";
						statement.executeUpdate(sql);
					}
					else {
						sql = "CREATE TABLE "+Table+" ("+ColumnNames.get(0)+" "+ColumnTypes.get(0)+");";
						statement.executeUpdate(sql);
					}
					
					for (int i = 1; i < count; i++)
					{
					    if (ColumnNames.get(i).equals(prevColName))
					    {
					        sql = "ALTER TABLE "+Table+" ADD COLUMN "+ColumnNames.get(i)+" String;";
					        statement.executeUpdate(sql);
					    }
					    else
					    {
					    	sql = "ALTER TABLE "+Table+" ADD COLUMN "+ColumnNames.get(i)+" "+ColumnTypes.get(i)+";";
					    	statement.executeUpdate(sql);
					    }
					} 
					
					sql = "INSERT INTO "+Table+" SELECT * from tempTable;";
					statement.executeUpdate(sql);
					
					for(int i=0; i < count; i++) {  //cache the indexes that were in the original table
						int check = checkIndex(ColumnNames.get(i), "tempTable", OldColumn, Column);
						if(check == 1) {
							cacheIndexNames.add(oldIndexName);
							cacheIndexColumns.add(oldIndexColumnName);
						}
					}
					
					sql = "DROP TABLE tempTable";
					statement.executeUpdate(sql);
					
					for(int i=0; i<cacheIndexNames.size(); i++){ //restore indexes for the new table
						
						sql = "CREATE INDEX "+cacheIndexNames.get(i)+" on "+Table+"("+cacheIndexColumns.get(i)+");";
						statement.executeUpdate(sql);
						System.out.println(sql);
					
				}
					/*
					 * sql = "UPDATE sqlite_master set sql="CREATE TABLE "+Table+" " -- Alternate method
					 */
				rs.close();
				statement.close();
				c.close();
				System.out.println("\nOperation Executed.");
			} catch (SQLException e) {
				System.out.println("Error. Table/Column not found.");
			} 
			
		}
		
		public boolean checkIfPrimaryKey(String tableName, String columnName) throws Exception {
		    int pk = 999;

		    Class.forName("org.sqlite.JDBC");
		    c = DriverManager.getConnection("jdbc:sqlite:test.db");
		    statement = c.createStatement();
		    ResultSet rs = statement.executeQuery( "PRAGMA table_info(" + tableName + ");" );

		    while ( rs.next() ) {
		        if (columnName.equals(rs.getString("name"))) {
		           pk = rs.getInt("pk");
		        }
		    }

		    rs.close();
		    return pk==1 ? true : false;
	    }
		
		public int checkIndex(String currentColumn, String Table, String renamedColumn, String newColName) throws Exception {
			ArrayList<String> Index = new ArrayList<String>();
			try {
				ResultSet ri = statement.executeQuery("PRAGMA index_list(tempTable);");
				int count = 0;
				while(ri.next()) {
					count++;   //count of rs is required.  Resultset is forward only.
				}
				ri.close();
			    ResultSet r2 = statement.executeQuery("PRAGMA index_list(tempTable);");
				for(int i=0; i < count; i++) {   //store pragma index list in arraylist to avoid inconsistent state error
					System.out.println("PK already added.");
					String indexInfo = r2.getString("name");
					if(indexInfo.contains("sqlite_autoindex")){
						
					}
					else {
						Index.add(indexInfo);
					}
				//	System.out.println("Added to list: "+Index.get(i));
					if(r2.next()) {
						r2.next();
					}
				} 
				r2.close();
				for(int i=0; i<Index.size(); i++) {
						String indexName = Index.get(i);
						ResultSet r = statement.executeQuery("PRAGMA index_info("+indexName+");");
						String indexColumnName = r.getString("name");
						if(indexColumnName.equals(currentColumn)) {  //if index found
							indexFlag = 1;
							if(indexColumnName.equals(renamedColumn)){ //if column is the one user is renaming
								oldIndexName = indexName;
								oldIndexColumnName = newColName;  //make sure index is created on new column name, not old
							}
							else {
								oldIndexName = indexName;
								oldIndexColumnName = currentColumn;
							}
							System.out.println("Index detected on given column: "+currentColumn);
						
								return 1;
					
						}
				}  
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return 0;
		}
}
