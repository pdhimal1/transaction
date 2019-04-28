import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface that the transaction replicas provide to the Transaction Manager (TM).
 *
 * Transaction manager sits in the transaction server application. The transaction manager
 * does not have a data store of its own. It uses the replicas to fulfil client's requests.
 *
 * @author dhimal
 */
public interface TransactionInterfaceReplicaToTM extends Remote
{
    /**
     * If the client is trying to query something (a key), the Transaction Manager (TM)
     * randomly selects a replica from the list of available replicas, and issues the request.
     * @param key to query
     * @return
     * @throws RemoteException
     */
	String get(String key) throws RemoteException;


    /**
     * When the transaction manager need to collect votes from all the replicas it simply invokes this method.
     * A replica will:
     *   - return true if its up and running
     *   - not return anything (which will be counted as a no vote (false).
     *
     * When the server doesnot hear back from one of the replicas, it invokes the test() method below
     * to test weather the replica is online and can be used for future requests.
     *
     * @param transaction
     * @return
     * @throws RemoteException
     */
	boolean phaseOneRequest(Transaction transaction) throws RemoteException;


    /**
     * After the voting is done and all of the replicas replied to commit the transaction,
     * this method is invoted to actually commit the transaction. This is phase two of the
     * two phase commit process.
     *
     * @param transaction
     * @return
     * @throws RemoteException
     */
	boolean commit(Transaction transaction) throws RemoteException;

    /**
     * After the voting is done and at least one of the replicas failed to reply to commit the transaction,
     * this method is invoted to abort the transaction. This is phase two of the
     * two phase commit process.
     *
     * There is actually no operation required to abort the transaction on the replicas because the transaction was
     * never commited. The transaction is simply "dropped".
     *
     * @param transaction
     * @return
     * @throws RemoteException
     */
	boolean abort(Transaction transaction) throws RemoteException;


    /**
     * During the voting process, if the Transaction Manager (TM) does not hear back from one of the replicas, it will
     * invoke test() to see if this replica is running and could be used for future transactions.
     *
     * If the Transaction Manager does not hear back from this replica during the test process, the replica will be removed
     * from the list of available replicas.
     *
     * @throws RemoteException
     */
	void test() throws RemoteException;
}
