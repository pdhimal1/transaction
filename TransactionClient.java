import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client application to use the two phase commit system store, delete, and lookup a table.
 *
 * Takes in the following arguments from the user:
 * 		"1. The port number this UDP Client should be running at.\n" +
 * 		"2. The IP address of the UDP server.\n" +
 * 		"3. The port number of the UDP server.\n" +
 * 		"4. debug option\n");
 *
 * Example: ./transactionClient.sh 6501 192.168.1.168 6500 debug
 *
 * @author dhimal
 */
public class TransactionClient
{
	private static int DEFAULT_SERVER_PORT = 6500;


	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		Logger.getGlobal().info("Starting client ..");

		int clientPort;

		String serverIPAddress = "localhost";
		int serverPort = DEFAULT_SERVER_PORT;

		boolean debug = false;
		if (args.length > 4)
		{
			Logger.getGlobal().warning("Only expecting four or less argument.");
			return;
		}
		else if (args.length != 0)
		{
			try
			{
				clientPort = Integer.parseInt(args[0]);
				Logger.getGlobal().info("Using " + clientPort + " as the client's port number");
			}
			catch (NumberFormatException e)
			{
				Logger.getGlobal().warning("Cannot parse " + args[0] + " as a port number.");
				return;
			}

			serverIPAddress = args[1];
			Logger.getGlobal().info("Using " + serverIPAddress + " as the server's IP address");

			try
			{
				serverPort = Integer.parseInt(args[2]);
				Logger.getGlobal().info("Using " + serverPort + " as the server's port number");
			}
			catch (NumberFormatException e)
			{
				Logger.getGlobal().warning("Cannot parse " + args[2] + " as a port number.");
				return;
			}

			if (args.length > 3 && args[3].equalsIgnoreCase("debug"))
			{
				debug = true;
			}
		}
		else
		{
			Logger.getGlobal().severe(
				"This program takes up to three arguments.\n" +
					"1. The port number this UDP Client should be running at.\n" +
					"2. The IP address of the UDP server.\n" +
					"3. The port number of the UDP server.\n" +
					"4. debug option\n");
			Logger.getGlobal().severe("Exiting now.");
			System.exit(0);
		}

		if (debug)
		{
			Logger.getGlobal().setLevel(Level.ALL);
		}
		else
		{
			Logger.getGlobal().setLevel(Level.WARNING);
		}

		try
		{
			TransactionInterfaceServerToClient remoteObject = (TransactionInterfaceServerToClient) Naming.lookup(
				"rmi://" + serverIPAddress + ":" + serverPort + TransactionServer.SERVER_RMI_LOCATION);

			// Logger.getGlobal().info("Message received : " + remoteObject.getInformation());

			Scanner scanner = new Scanner(System.in);
			int option;
			while (true)
			{
				try
				{
					System.out.println("\nSelect an option from the following Menu: ");
					System.out.println("1. Insert a key value pair");
					System.out.println("2. Delete a key");
					System.out.println("3. Lookup using a key");
					System.out.println("4. Exit.");

					option = scanner.nextInt();
					String key;

					if (option == 1)
					{
						System.out.println("Enter the key:");
						key = scanner.next();

						System.out.println("Enter the value");
						String value = scanner.next();

						if (!remoteObject.put(key, value))
						{
						    // TODO - is this really happening?
							Logger.getGlobal().severe("Looks like the transaction didn't go through. Try again.");
						}
					}
					else if (option == 2)
					{
						System.out.println("Enter the key:");
						key = scanner.next();

						if (!remoteObject.del(key))
						{
						    // TODO - is this really happening
							Logger.getGlobal().severe("Looks like the transaction didn't go through. Try again.");
						}
					}
					else if (option == 3)
					{
						System.out.println("Enter the key:");
						key = scanner.next();

						String value = remoteObject.get(key);

						// todo - how does the client know if it fails?
						System.out.println(value);
					}
					else if (option == 4)
					{
						Logger.getGlobal().info("Exiting the program.");
						System.exit(0);
					}
				}
				catch (InputMismatchException e)
				{
					scanner.next();
					Logger.getGlobal().severe("Input mismatch.");
				}
			}
		}
		catch (RemoteException | NotBoundException | MalformedURLException e)
		{
			Logger.getGlobal().severe("Could not contact the server.");
			System.exit(0);
		}
	}
}
