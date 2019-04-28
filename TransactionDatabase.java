import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Provides key/value store functionality to the replica servers.
 *
 * This is a durable data store as described in the project summary:
 * <pre>
 * In this part of the assignment, your job is to implement a durable key/value store library.
 * Your library should expose three methods:
 *  - put(key, value): stores the value "value" with the key "key".
 *  - del(key): deletes any record associated with the key "key".
 * 	- value = get(key): retrieves and returns the value associated with the key "key".
 * </pre>
 *
 * Database:
 * - create a database
 * - create a table on the database
 * - add key,value to the database
 * - remove key,value from the database
 * - query key,value from the database
 *
 *
 * @author dhimal
 */
public class TransactionDatabase
{
	static String DIRECTORY = "sqlite-db";

	static String FILENAME = "transaction-data";

	static String FILE_EXT = ".db";

	private File directory;

	private String database_url;

	private Connection connection;


	/**
	 *
	 * @param databaseDirectory the directory this database should write on
	 * @param dabataseFilePath the name of the database file this application should use.
	 */
	TransactionDatabase(String databaseDirectory, String dabataseFilePath)
	{
		// Database directory is the absolute path from this program
		createDatabaseDirectory(databaseDirectory);
		File databaseFile = new File(directory, dabataseFilePath);

		this.database_url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

		createNewDatabase();
		createNewTable();
	}


	/**
	 * Create new database - this is actually called only once during the application lifecycle.
	 * If this is invoked on an existing data store (database file), then the application can re-use the
	 * data stored in the previous session.
	 * @param databaseDir
	 */
	private void createDatabaseDirectory(String databaseDir)
	{
		File databaseDirectory = new File(databaseDir);
		if (!databaseDirectory.exists() && !databaseDirectory.mkdirs())
		{
			Logger.getGlobal().severe("Could not create a directory " + databaseDirectory.getAbsolutePath());
		}
		this.directory = databaseDirectory;
	}


	/**
	 * Gets the connection to the SQLite database.
	 * @return
	 */
	private Connection connection()
	{
		if (connection == null)
		{
			connection = connect();
		}
		return connection;
	}


	/**
	 * Connect to the SQLite database.
	 *
	 * The database URL is set once per session.
	 *
	 * @return the Connection object
	 */
	private Connection connect()
	{
		// SQLite connection string
		Connection connection = null;
		try
		{
			connection = DriverManager.getConnection(database_url);
		}
		catch (SQLException e)
		{
			Logger.getGlobal().info("Could not connect to the database at " + database_url);
			e.printStackTrace();
		}
		return connection;
	}


	/**
	 * Inserts key/value into the database. This method is invoked by the replicas.
	 *
	 * @param keyString
	 * @param valueString
	 */
	void insert(String keyString, String valueString)
	{
		String sql = "INSERT INTO KEYVALUE(k,v) VALUES(?, ?)";
		PreparedStatement preparedStatement = null;
		try
		{
			preparedStatement = connection().prepareStatement(sql);
			preparedStatement.setString(1, keyString);
			preparedStatement.setString(2, valueString);
			preparedStatement.executeUpdate();
			Logger.getGlobal().info(keyString + ":" + valueString + " inserted into the database");
		}
		catch (SQLException e)
		{
			if (e.toString().contains("UNIQUE constraint failed"))
			{
				Logger.getGlobal().severe(keyString + " is not a unique key to this database");
			}
			Logger.getGlobal().severe("Could not insert " + keyString + ":" + valueString);
		}
		finally
		{
			close(preparedStatement);
		}
	}


	/**
	 * Deletes a key/value pair using the key from the database. This method is invoked by the replicas.
	 *
	 * @param keyString
	 */
	void delete(String keyString)
	{
		String sql = "DELETE FROM KEYVALUE where k = ?";
		PreparedStatement preparedStatement = null;
		try
		{
			preparedStatement = connection().prepareStatement(sql);
			preparedStatement.setString(1, keyString);
			preparedStatement.executeUpdate();
			// todo - deleting a key that was not existent is not a problem?
			Logger.getGlobal().info(keyString + " deleted from the database");
		}
		catch (SQLException e)
		{
			Logger.getGlobal().severe("Could not remove " + keyString);
		}
		finally
		{
			close(preparedStatement);
		}
	}


	/**
	 * Retrieves the value using the given key from the database. This method is invoked by the replicas.
	 * @param keyString
	 * @return
	 */
	String get(String keyString)
	{
		String sql = "SELECT v FROM KEYVALUE where k = ?";
		PreparedStatement preparedStatement = null;
		try
		{
			preparedStatement = connection().prepareStatement(sql);
			preparedStatement.setString(1, keyString);
			ResultSet resultSet = preparedStatement.executeQuery();
			String value = resultSet.getString("v");
			Logger.getGlobal().info(keyString + " queried from the database");
			return value;
		}
		catch (SQLException e)
		{
			Logger.getGlobal().severe("Could not query " + keyString);
			return "";
		}
		finally
		{
			close(preparedStatement);
		}
	}


	/**
	 * Utility method to close the prepared statements.
	 *
	 * @param statement
	 */
	private static void close(Statement statement)
	{
		try
		{
			if (statement != null)
			{
				statement.close();
			}
		}
		catch (SQLException e)
		{
			Logger.getGlobal().severe("Could not close statement.");
		}
	}


	/**
	 * Create a new table - this is actually called only once during the application lifecycle.
	 *
	 * If this is invoked on an existing data store (database file), then the application can re-use the
	 * data stored in the previous session.
	 *
	 * The name of the table here is KEYVALUE, has the following:
	 *   - k of type TEXT that is also a primary key. K cannot be null;
	 *   - v of type TEXT. v cannot be null;
	 *
	 */
	private void createNewTable()
	{
		// SQL statement for creating a new table
		String sql = "CREATE TABLE IF NOT EXISTS KEYVALUE"
			+ "(k TEXT PRIMARY KEY NOT NULL,"
			+ "	v TEXT NOT NULL"
			+ ");";
		try
		{
			Statement statement = connection().createStatement();
			statement.execute(sql);
			Logger.getGlobal().info("New table KEYVALUE has been created");
		}
		catch (SQLException e)
		{
			Logger.getGlobal().severe("Could not create a table.");
		}
	}


	private void createNewDatabase()
	{
		try
		{
			if (connection() != null)
			{
				DatabaseMetaData meta = connection().getMetaData();
				Logger.getGlobal().info("The driver name is " + meta.getDriverName());
				Logger.getGlobal().info("A new database has been created in " + database_url);
			}
		}
		catch (SQLException e)
		{
			Logger.getGlobal().severe(e.toString());
		}
	}


	/**
	 * Method to show all values from the database.
	 *
	 * This method is not used by the replicas. It could be used for testing purposes.
	 */
	private void showAll()
	{
		String query = "SELECT * FROM KEYVALUE;";
		try
		{
			Statement statement = connection().createStatement();
			ResultSet resultSet = statement.executeQuery(query);

			while (resultSet.next())
			{
				String key = resultSet.getString("k");
				String value = resultSet.getString("v");
				Logger.getGlobal().info(key + ":" + value);
			}
		}
		catch (SQLException e)
		{
			Logger.getGlobal().severe("Could not show all.");
		}
	}


	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args)
	{
		TransactionDatabase app = new TransactionDatabase("sqlite-db", "db1.db");
		app.createNewDatabase();
		app.createNewTable();

		app.insert("key1", "value1");
		app.insert("key2", "value2");
		app.insert("key2", "value3");

		String query = app.get("key2");

		app.showAll();
	}
}
