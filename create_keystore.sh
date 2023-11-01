#!/bin/bash
# https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG179

# Generate the self-signed certificate and place it in the KeyStore
keytool -genkey -noprompt \
 -alias "${ALIAS}" \
 -dname "CN=mqttserver.ibm.com, OU=ID, O=IBM, L=Hursley, S=Hants, C=US" \
 -keystore "${KEYSTORE}" \
 -storepass "${PASSWORD}" \
 -keypass "${PASSWORD}" \
 -keyalg RSA \
 -sigalg SHA256withRSA

# Export the certificate needs to a certificate file, use this on both client and server to run
# Technically you just need the key store between level-sites, instead of hashing, just encrypt the leaf value
# with Paillier?

# -Djavax.net.ssl.keyStore=${KEYSTORE} -Djavax.net.ssl.keyStorePassword=${PASSWORD}
# -Djavax.net.ssl.trustStore=${KEYSTORE} -Djavax.net.ssl.trustStorePassword=${PASSWORD}

# keytool -export -alias "${ALIAS}" -keystore "${KEYSTORE}" -rfc -file "${CERTIFICATE}" -storepass "${PASSWORD}"
