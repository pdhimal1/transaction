#!/bin/sh

javac TransactionClient.java

# ./transactionClient.sh 6501 localhost 6500 debug
# ./transactionClient.sh <client-port> <server-ip-address> <server-port> <debug-optional>
java TransactionClient "$@"
