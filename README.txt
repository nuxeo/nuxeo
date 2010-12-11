Nuxeo-Platform-Signature: a digital signature plugin for signing PDF files.

Random notes about this project:

PROJECT STRUCTURE
This project can be divided conceptually into 3 parts:

1) certificate generation (low-level PKI object operations, CA operations)
2) certificate persistence (storing and retrieving keystores containing certificates inside nuxeo directories)
3) pdf signing with a certificate


PREREQUISITES:

1) Install your root keystore file in a secured directory

To do initial testing you can use the following keystore: 
./nuxeo-platform-signature-core/src/test/resources/test-files/keystore.jks

using the location and password specified in:
./nuxeo-platform-signature-core/src/main/resources/OSGI-INF/cert-contrib.xml

2) Install libraries (TODO: these will be provided in the distribution)

The iText and the BouncyCastle libraries need to be copied into the library folder of your app server:
Copy the following files:  
- itext-2.0.7.jar
- bcprov-jdk14-136.jar

into the following directory of your respective server:
- jboss:  <jboss>/server/default/deploy/nuxeo.ear/lib
- tomcat: <tomcat>/nxserver/lib

3) Modify your database

Modify your "cert" table "keystore" column to be large enough to contain an encoded keystore 
-- ALTER TABLE cert DROP COLUMN keystore;
ALTER TABLE cert ADD COLUMN keystore text;

4) You might have to modify your system's java encryption configuration, 
for instance by installing JCE Unlimited Strength Jurisdiction Policy Files needed for passwords longer than 7 characters,

###################################################################################################################################################
# Note: cryptography exportation laws differ between countries so make sure you are using adequate encryption configuration, libraries and tools. #
###################################################################################################################################################


IMPLEMENTATION DETAILS - TODO:

1) Do we need both: a keystore password and a key password for each user ?
Keystore is used in a non-standard way in this project. Each user's certificate/private key pair is stored in a separate keystore.

Multiple keystores vs single keystore for all certificates/keys: 
Advantages:
- keystore file locking for writing : no need for concurrent access to the same keystore file, 
- keystore corruption: a single keystore file corrupted or tampered with/broken does not break other keystores, 
- no shared password/no single point of entry issues: a revealed password for one keystore does not affect other keystore
- a keystore password can be changed without affecting legacy keystores. 
- upgrades: a new type of keystore could be introduced with different encryption, which would also not affect legacy keystores. 
Disadvantages:
- fast directory size growth for all keystores to be maintained (single keystore is more space-efficient than multiple ones)
- not intuitive / non-standard approach
- performance could suffer
- ?

2) Not implemented yet - certificate end/expiration date configuration. This has been set to 12 months now, that is a default certificate will expire 12 months from its creation.  This can be easily implemented. 

3) Prepare the distribution package
a) copy required libraries
- iText
- BouncyCastle


