```
Prakash Dhimal
George Mason University
Distributed Systems - CS675
Distributed Systems Course Project: Two-Phase Commit
A simple transaction system
```

Application suite:

There are three applications that interact over a distributed network (medusa cluster) to simulate a simple transaction system:

   * A server application (TransactionServer.java), also referred to as the "master", that exposes transaction interface to the client application.
   A client can do the following: 
    * put(key, value): stores the value "value" with the key "key".
    * del(key): deletes any record associated with the key "key".
    * value = get(key): retrieves and returns the value associated with the key "key".
    
   The server application starts a transaction manager that is going to co-operate with the replicas to store, delete, and 
   query for key and/or values.
    
   To start the server application: 
     * Navigate to the src directory.
     * Run `./transactionServer.sh`
        * Format: `./transactionServer.sh <server-port> <debug-optional>`
        * Example: `./transactionServer.sh 6500 debug`
   
   Note: The server application does not need to know where the replicas are. The replicas will contact the server application
   to join the program once they are started. 
   
   Transaction server compiles the TransactionServer.java and its utility classes using the following command:
      * `javac TransactionServer.java`
   
   If you don't have java installed on your system, it is recommended that you use the already compiled files provided with this project. 
    
   * A replica application that actually stores the key/value pair in a data store of its own. This data store is designed to be durable; 
   i.e if the replica application is down, it should restore those key/value pairs stored from the previous session. The replica application/s
   need to know about the existence and whereabouts of the server application when they start. To make things simple on the server side, 
   the replica application will contact the server application and establish itself as one of the replicas to provide the data store.
   
   To run the replica application:
     * Navigate to the src directory.
     * Run `./transactionReplica.sh`
       * Format: `./transactionReplica.sh <replica-port> <server-ip-address> <server-port> <database-dir> <database-file> <debug-optional>`
       * Example: `./transactionReplica.sh 6501 192.168.1.168 6500 n/a n/a debug`
       * Example: `./transactionReplica.sh 6501  192.168.1.168 6500 sqlite-db transaction-db.db deug`
       
   The server application needs to be up and running when they replicas are started. 
   
   Users (maintainers because replica is not a client facing application) also need to provide the path to the datafile 
   store (SQLite database file if it already exists). The default data-store file is `transaction-data.db` within the `sqlite-db` 
   directory of this project's current working directory. If the replicas are running on cluster of nodes that share the same file system, 
   it is important to use different file per replica.
   
   Replica application maintains a stable data store (database) using SQLite database library. The SQLite JDBC Driver
   JAR (`sqlite-jdbc-3.27.2.1.jar`) is provided with the project. This JAR needs to be included in the classpath, 
   which the `transactionReplica.sh` takes care of by running `export CLASSPATH=${CLASSPATH}:/java/classes:${curDir}/sqlite-jdbc-3.27.2.1.jar`.
       
   * A client application that the clients will use to interact with the transaction server. As mentioned above, 
    A client can do the following: 
       * put(key, value): stores the value "value" with the key "key".
       * del(key): deletes any record associated with the key "key".
       * value = get(key): retrieves and returns the value associated with the key "key".

   To start the server application: 
       * Navigate to the src directory.
       * Run `./transactionClient.sh`
         * Format: `./transactionClient.sh <client-port> <server-ip-address> <server-port> <debug-optional>`
         * Example: `./transactionClient.sh 6505 192.168.1.168 6500 debug`  