Nuxeo-Platform-Signature: a digital signature plugin for signing PDF files.

PROJECT STRUCTURE
This project can be divided conceptually into 3 parts:

1) certificate generation (low-level PKI object operations, CA operations)
2) certificate persistence (storing and retrieving keystores containing certificates inside nuxeo directories)
3) pdf signing with an existing certificate


CONFIGURATION:

1) Install your root keystore file in a secured directory

To do initial testing you can use the following keystore: 
./nuxeo-platform-signature-core/src/test/resources/test-files/keystore.jks

using the location, aliases and password specified in:
./nuxeo-platform-signature-core/src/main/resources/OSGI-INF/root-contrib.xml


2) You might have to modify your server system's java encryption configuration
by installing JCE Unlimited Strength Jurisdiction Policy Files needed for passwords longer than 7 characters,

###################################################################################################################################################
# Note: cryptography exportation laws differ between countries so make sure you are using adequate encryption configuration, libraries and tools. #
###################################################################################################################################################

