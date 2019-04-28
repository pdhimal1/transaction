#!/bin/sh

javac TransactionServer.java

# ./transactionServer.sh 6500 debug
# ./transactionServer.sh <server-port> <debug-optional>
java TransactionServer "$@"
