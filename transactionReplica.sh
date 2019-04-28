#!/bin/sh
curDir=`pwd`

javac TransactionReplica.java

# ./transactionReplica.sh 6501  192.168.1.168 6500 sqlite-db transaction-db.db debug
# ./transactionReplica.sh 6502 192.168.1.168 6500 n/a n/a debug
# ./transactionReplica.sh <replica-port> <server-ip-address> <server-port> <database-dir> <database-file> <debug-optional>
export CLASSPATH=${CLASSPATH}:/java/classes:${curDir}/sqlite-jdbc-3.27.2.1.jar
java TransactionReplica "$@"
