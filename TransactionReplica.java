import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionReplica extends UnicastRemoteObject implements TransactionInterfaceReplicaToTM
{
	static String REPLICA_RMI_LOCATION = "/replica";

	private TransactionDatabase transactionDatabase;


	private TransactionReplica(String databaseDir, String databaseFilePath) throws RemoteException
	{
		super();
		this.transactionDatabase = new TransactionDatabase(databaseDir, databaseFilePath);
	}


	private TransactionDatabase twoPhaseCommitDatabase()
	{
		return transactionDatabase;
	}


	@Override
	public String get(String key)
	{
		return twoPhaseCommitDatabase().get(key);
	}


	@Override
	public boolean phaseOneRequest(Transaction transaction)
	{
		// decide if the transaction can go trough

		// Todo - how do I decide when a transaction can't be commited?
		return true;
	}


	@Override
	public boolean commit(Transaction transaction)
	{
		// commit the transaction on your local
		if (Transaction.TransactionType.INSERT.equals(transaction.transactionType()))
		{
			twoPhaseCommitDatabase().insert(transaction.getKey(), transaction.getValue());
		}
		else if (Transaction.TransactionType.DELETE.equals(transaction.transactionType()))
		{
			twoPhaseCommitDatabase().delete(transaction.getKey());
		}
		return true;
	}


	@Override
	public boolean abort(Transaction transaction)
	{
		// todo - do I need this?

		return true;
	}


	@Override
	public void test() throws RemoteException
	{
		// no op
	}


	private void contactServer(String ipAddress, int port, String myIP, int myPort)
	{
		try
		{
			TransactionInterfaceServerToClient server = (TransactionInterfaceServerToClient) Naming.lookup(
				"rmi://" + ipAddress + ":" + port + TransactionServer.SERVER_RMI_LOCATION);

			server.addNewReplica(myIP, myPort);
			Logger.getGlobal().info("Adding replica to the server at " + ipAddress + ":" + port);
		}
		catch (NotBoundException | MalformedURLException | RemoteException e)
		{
			Logger.getGlobal().severe("Could not add replica at " + ipAddress + ":" + port);
		}
	}


	public static void main(String[] args)
	{
		try
		{
			int replicaPort;
			boolean debug;

			Optional<String> serverIPAddress;
			Optional<Integer> serverPort;
			String filePath;
			String fileName;

			if (args.length != 6)
			{
				Logger.getGlobal().severe(
					"This program takes exactly six arguments.\n" +
						"1. The port number this transaction replica should be running at.\n" +
						"2. The IP address of the transaction server.\n" +
						"3. The port number of the transaction server.\n" +
						"4. The relative path to the database directory (n/a otherwise).\n" +
						"5. The name of the database file (n/a otherwise).\n" +
						"6. debug option (no otherwise)\n");
				Logger.getGlobal().severe("Exiting now.");
				System.exit(0);
			}

			try
			{
				replicaPort = Integer.parseInt(args[0]);
				Logger.getGlobal().info("Using " + replicaPort + " as the replica's port number");
			}
			catch (NumberFormatException e)
			{
				Logger.getGlobal().warning("Cannot parse " + args[0] + " as a port number.");
				return;
			}

			serverIPAddress = Optional.of(args[1]);
			Logger.getGlobal().info("Using " + serverIPAddress + " as the transaction server's IP address");

			try
			{
				serverPort = Optional.of(Integer.parseInt(args[2]));
				Logger.getGlobal().info("Using " + serverPort + " as the server's port number");
			}
			catch (NumberFormatException e)
			{
				Logger.getGlobal().warning("Cannot parse " + args[2] + " as a port number.");
				return;
			}

			/**
			 * If the file path and the name are not give, use the defaults.
			 */
			filePath = "n/a".equalsIgnoreCase(args[3]) ? TransactionDatabase.DIRECTORY : args[3];
			fileName = "n/a".equalsIgnoreCase(args[4]) ? TransactionDatabase.FILENAME + TransactionDatabase.FILE_EXT : args[4];

			Logger.getGlobal().warning("Using " + filePath + "/" + fileName + " as the database file");

			debug = !"no".equalsIgnoreCase(args[5]);

			if (debug)
			{
				Logger.getGlobal().setLevel(Level.ALL);
			}
			else
			{
				Logger.getGlobal().setLevel(Level.WARNING);
			}

			String ipAddress = InetAddress.getLocalHost().getHostAddress();
			Logger.getGlobal().info("Ipaddress " + ipAddress);

			// rmi registry -> create a registry for this service to run on?
			LocateRegistry.createRegistry(replicaPort);

			TransactionInterfaceReplicaToTM remoteObject = new TransactionReplica(filePath, fileName);

			// Bind the remote object (RMIImplementation) by name
			Naming.bind("rmi://" + ipAddress + ":" + replicaPort + REPLICA_RMI_LOCATION, remoteObject);

			// Naming.bind("rmi://localhost:6500" + "/game", twoPhaseRMIImplementation);
			Logger.getGlobal().info(
				TransactionReplica.class.getName() + " started at : " + ipAddress + ":" + replicaPort);

			if (serverIPAddress.isPresent() && serverPort.isPresent())
			{
				((TransactionReplica) remoteObject).contactServer(serverIPAddress.get(), serverPort.get(), ipAddress, replicaPort);
			}
		}
		catch (RemoteException | AlreadyBoundException | MalformedURLException |

				UnknownHostException e)
		{
			e.printStackTrace();
		}
	}
}
