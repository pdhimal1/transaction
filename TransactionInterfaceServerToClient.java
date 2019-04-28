import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An interface that the transaction server provides to the client (user-facing)
 * applications. As described in the project summary, a client shoud be able to do:
 *  - put(key, value): stores the value "value" with the key "key".
 *  - del(key): deletes any record associated with the key "key".
 *  - value = get(key): retrieves and returns the value associated with the key "key".
 *
 * There is one exception here: {@link addNewReplica(java.lang.String, int).
 * A replica will use this interface to join the server's list of available replicas.
 *
 * @author dhimal
 */
public interface TransactionInterfaceServerToClient extends Remote
{

	boolean put(String key, String value) throws RemoteException;


	boolean del(String key) throws RemoteException;


	String get(String key) throws RemoteException;


	void addNewReplica(String ipAddress, int port) throws RemoteException;
}
