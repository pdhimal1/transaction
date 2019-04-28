import java.io.Serializable;
import java.util.Optional;

/**
 * Holds information about the transaction
 *   - Type of transaction, see {@link TransactionType}
 *   - a key
 *   - a value or ""
 *   - the transaction ID
 */
class Transaction implements Serializable
{
	public enum TransactionType {
			DELETE, INSERT, GET
	};

	private int globalID;

	private TransactionType transactionType;

	private String key;

	private String value;


	Transaction(
			int globalID,
			TransactionType transactionType,
			String key,
			Optional<String> value)
	{
		this.globalID = globalID;
		this.key = key;
		this.transactionType = transactionType;
		this.value = value.orElse("");
	}


	public TransactionType transactionType()
	{
		return transactionType;
	}


	public String getKey()
	{
		return key;
	}


	public String getValue()
	{
		return value;
	}
}
