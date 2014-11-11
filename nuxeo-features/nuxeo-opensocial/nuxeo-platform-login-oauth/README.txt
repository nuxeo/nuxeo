1.) Generate an OpenSSL key using:

openssl genrsa -out openssl_key.pem 1024

2.) Convert to PKCS#8 format:

openssl pkcs8 -topk8 -in openssl_key.pem -inform pem -out openssl_key_pk8.pem -outform pem -nocrypt

3.) Remove the header and footer lines from openssl_key_pk8.pem. If openssl_key_pk8.pem is:

-----BEGIN PRIVATE KEY-----

...base64 encoded key...

-----END PRIVATE KEY-----

Then I was only able to make this work by deleting the "-----BEGIN PRIVATE KEY-----" and "-----END PRIVATE KEY-----" lines from the file. Not sure if this is a bug or if I'm going about this incorrectly, but it looks like this is because http://oauth.googlecode.com/svn/code/java/core/src/main/java/net/oauth/signature/RSA_SHA1.javaexpects a PEM encoded private key passed as a String to be a base64 encoded byte array without any additional data and doesn't strip any lines out before base64 decoding it. SigningFetcherFactory.java winds up loading the key file and passing it to the OAuth lib without stripping these either, hence the need to manually remove them.

4.) Edit java/gadgets/conf/gadgets.properties to point to openssl_key_pk8.pem:

signing.key-file=/path/to/openssl_key_pk8.pem

5.) mvn install and mvn -Prun as normal. Signed makeRequest calls from the samplecontainer will now function. 
6.) Generate public key
 openssl req -new -key openssl_key.pem -x509 -days 365 -out public-key.pem
