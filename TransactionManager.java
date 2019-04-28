import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

class TransactionManager
{
	private List<TransactionInterfaceReplicaToTM> replicas;

	private AtomicInteger transactionID;


	/**
	 * master: the "master" process should expose an RPC interface to clients that contains three methods:
	 * get, put, and del. When the master process receives a state-changing operation (del or put),
	 * it uses two-phase commit to commit that state-changing operation to all replicas.
	 * When the master receives a "get" operation, it selects a replica at random to issue the request against.
	 * <p>
	 */
	TransactionManager()
	{
		transactionID = new AtomicInteger();
		replicas = new ArrayList<>();
	}


	private int getNextTransactionID()
	{
		return transactionID.incrementAndGet();
	}


	private List<TransactionInterfaceReplicaToTM> getReplicas()
	{
		return replicas;
	}


	void addReplica(TransactionInterfaceReplicaToTM replica)
	{
		getReplicas().add(replica);
	}


	private void removeReplicas(List<TransactionInterfaceReplicaToTM> replica)
	{
		getReplicas().removeAll(replica);
	}


	private int numberOfReplicas()
	{
		return getReplicas().size();
	}


	private int getRandomInt(int max)
	{
		Random random = new Random();
		return random.nextInt(max);
	}


	/**
	 * When the master receives a "get" operation, it selects a replica at random to issue the request against.
	 *
	 * @return value associated with the key
	 */
	String get(String key)
	{
		/**
		 * 1. select a random replica
		 * 2. Issue the request
		 * 2. Return the result
		 */
		if (numberOfReplicas() == 0)
		{
			Logger.getGlobal().severe("No replicas available");
			return "";
		}
		// This should handle when there is only one replica
		int random = getRandomInt(numberOfReplicas());

		try
		{
			return getReplicas().get(random).get(key);
		}
		catch (RemoteException e)
		{
			Logger.getGlobal().severe("Remote exception on get " + key + " request. Try again.");
		}
		return "";
	}


	private Transaction getTransaction(String key, String value, Transaction.TransactionType transactionType)
	{
		return new Transaction(
			getNextTransactionID(),
			transactionType,
			key,
			Optional.of(value));
	}


	private Transaction getTransaction(String key, Transaction.TransactionType transactionType)
	{
		return new Transaction(
			getNextTransactionID(),
			transactionType,
			key,
			Optional.empty());
	}


	/**
	 * When the master process [Transaction Manager] receives a state-changing operation (del or put),
	 * it uses two-phase commit to commit that state-changing operation to all replicas.
	 *
	 * @return
	 */
	boolean del(String key)
	{
		// Create a transaction
		Transaction transaction = getTransaction(key, Transaction.TransactionType.DELETE);

		return phaseTwo(phaseOne(transaction), transaction);
	}


	/**
	 * When the master process [Transaction Manager] receives a state-changing operation (del or put),
	 * it uses two-phase commit to commit that state-changing operation to all replicas.
	 *
	 * @return
	 */
	boolean put(String key, String value)
	{
		Transaction transaction = getTransaction(key, value, Transaction.TransactionType.INSERT);

		return phaseTwo(phaseOne(transaction), transaction);
	}


	private boolean phaseOne(Transaction transaction)
	{
		List<Boolean> votes = collectVotes(transaction);
		return !votes.contains(Boolean.FALSE);
	}


	private List<Boolean> collectVotes(Transaction transaction)
	{
		List<Boolean> votes = new ArrayList<>();
		List<TransactionInterfaceReplicaToTM> replicasToRemove = new ArrayList<>();

		getReplicas().forEach(replica -> {
			try
			{
				votes.add(replica.phaseOneRequest(transaction));
			}
			catch (RemoteException e)
			{
				Logger.getGlobal().severe("Could not collect vote from this replica");
				votes.add(false);
				try
				{
					replica.test();
				}
				catch (RemoteException e1)
				{
					replicasToRemove.add(replica);
					Logger.getGlobal().severe("Replica removed after unsuccessful test");
				}
			}
		});
		removeReplicas(replicasToRemove);

		return votes;
	}


	private boolean phaseTwo(boolean phaseOneResult, Transaction transaction)
	{
		if (phaseOneResult)
			return commit(transaction);
		else
			return abort(transaction);
	}


	private boolean abort(Transaction transaction)
	{
		Logger.getGlobal().severe("Transaction aborted: " + transaction.transactionType() + " "
			+ transaction.getKey() + " " + transaction.getValue());
		Logger.getGlobal().info("Try again");
		return false;
	}


	private boolean commit(Transaction transaction)
	{
		// commit on all replicas?
		getReplicas().forEach(replica -> {
			try
			{
				replica.commit(transaction);
			}
			catch (RemoteException e)
			{
				Logger.getGlobal().severe("Could not commit the transaction");
			}

		});
		return true;
	}
}
