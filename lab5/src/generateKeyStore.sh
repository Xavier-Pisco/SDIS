#!/bin/bash

if [ $# -ne 4 ]
then
    echo "Usage: $0 <keystorefilename> <truststorefilename> <keystorealias> <truststorealias>";
    exit
fi

keytool -genkeypair -alias $3 -keyalg RSA  -validity 7 -keystore $1
keytool -export -alias $3 -keystore $1 -rfc -file $3.cer 
keytool -import -alias $4 -file $3.cer -keystore $2

