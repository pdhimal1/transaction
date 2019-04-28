import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionServer extends UnicastRemoteObject implements TransactionInterfaceServerToClient
{
	private static int DEFAULT_SERVER_PORT = 6500;

	static String SERVER_RMI_LOCATION = "/transactionServer";

	/**
	 * This is the client facing server
	 *
	 * - puts its RMI Interface to the client
	 *
	 *
	 * - "Discovers the Replicas" or start them up?
	 *
	 * Hands the information over to the transaction manager
	 */

	private TransactionManager transactionManager;


	private TransactionServer() throws RemoteException
	{
		transactionManager = new TransactionManager();
	}


	private TransactionManager transactionManager()
	{
		return transactionManager;
	}


	private void addReplica(String ipAddress, int port)
	{
		try
		{
			TransactionInterfaceReplicaToTM replica = (TransactionInterfaceReplicaToTM) Naming.lookup(
				"rmi://" + ipAddress + ":" + port + TransactionReplica.REPLICA_RMI_LOCATION);

			transactionManager().addReplica(replica);
			Logger.getGlobal().info("Adding replica at " + ipAddress + ":" + port);
		}
		catch (NotBoundException | MalformedURLException | RemoteException e)
		{
			Logger.getGlobal().severe("Could not add replica at " + ipAddress + ":" + port);
		}
	}


	@Override
	public boolean put(String key, String value) throws RemoteException
	{
		return transactionManager().put(key, value);
	}


	@Override
	public boolean del(String key) throws RemoteException
	{
		return transactionManager().del(key);
	}


	@Override
	public String get(String key) throws RemoteException
	{
		return transactionManager().get(key);
	}


	@Override
	public void addNewReplica(String ipAddress, int port) throws RemoteException
	{
		addReplica(ipAddress, port);
	}


	/**
	 * Create a transaction server and put it out there for the clients
	 * 
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		try
		{
			Map defaultMap = new HashMap();
			defaultMap.put("port", Integer.toString(DEFAULT_SERVER_PORT));
			defaultMap.put("debug", Boolean.toString(Boolean.FALSE));
			Map<String, String> portDebug_Map = extractPortAndDebug(args, DEFAULT_SERVER_PORT)
					.orElse(defaultMap);

			int port = Integer.valueOf(portDebug_Map.get("port"));
			boolean debug = Boolean.valueOf(portDebug_Map.get("debug"));

			if (!debug)
				Logger.getGlobal().setLevel(Level.WARNING);
			else
				Logger.getGlobal().setLevel(Level.ALL);

			String ipAddress = InetAddress.getLocalHost().getHostAddress();
			Logger.getGlobal().info("Ipaddress " + ipAddress);

			TransactionInterfaceServerToClient remoteObject = new TransactionServer();

			// rmi registry -> create a registry for this service to run on?
			LocateRegistry.createRegistry(port);

			// Bind the remote object (RMIImplementation) by name
			Naming.bind("rmi://" + ipAddress + ":" + port + SERVER_RMI_LOCATION, remoteObject);

			// Naming.bind("rmi://localhost:6500" + "/game", twoPhaseRMIImplementation);
			Logger.getGlobal().info(
				TransactionServer.class.getName() + " started at : " + ipAddress + ":" + port);
		}
		catch (RemoteException | AlreadyBoundException | MalformedURLException | UnknownHostException e)
		{
			e.printStackTrace();
		}
	}


	@SuppressWarnings("unchecked")
	private static Optional<Map<String, String>> extractPortAndDebug(String[] args, int defaultPort)
	{
		if (args.length > 2)
		{
			Logger.getGlobal().warning("Only expecting two or less argument.");
			return Optional.empty();
		}
		else if (args.length != 0)
		{
			int port = defaultPort;
			boolean debug = false;

			for (String arg : args)
			{
				try
				{
					port = Integer.parseInt(arg);
					Logger.getGlobal().info("Port number :" + port);
				}
				catch (NumberFormatException e)
				{
					if (arg.equalsIgnoreCase("debug"))
					{
						debug = true;
					}
					else
					{
						Logger.getGlobal().warning("Cannot parse " + args[0] + " as a port number and is not DEBUG.");
					}
				}
			}
			Map map = new HashMap();
			map.put("port", Integer.toString(port));
			map.put("debug", Boolean.toString(debug));
			return Optional.of(map);
		}
		else
		{
			Logger.getGlobal().severe(
				"This program takes up to two arguments.\n" +
					"1. The port number this transaction server should be running at.\n" +
					"2. debug option\n");
			Logger.getGlobal().severe("Exiting now.");
			System.exit(0);
		}
		return Optional.empty();
	}
}
