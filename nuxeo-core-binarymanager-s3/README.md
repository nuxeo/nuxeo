This addon implements a BinaryManager that stores binaries in a S3 bucket.
For efficiency, a local disk cache (with limited size) is also used.

Be sure to protect your nuxeo.conf (readable only by the nuxeo user) as the
file will have your AWS identifiers (unless you are using instance roles).

# Mandatory parameters

- nuxeo.core.binarymanager=org.nuxeo.ecm.core.storage.sql.S3BinaryManager

- nuxeo.s3storage.bucket : the name of the S3 bucket (unique across all of
  Amazon, find something original!)

- nuxeo.s3storage.awsid : your AWS_ACCESS_KEY_ID

- nuxeo.s3storage.awssecret : your AWS_SECRET_ACCESS_KEY

If the awsid and/or awssecret are not set, the addon will try to use
temporary credentials from the instance role (if any).


# Optional parameters

- nuxeo.s3storage.region : the region code your S3 bucket will be placed in.
  For us-east-1 (the default), don't set this parameter
  For us-west-1 (Northern California), use us-west-1
  For us-west-2 (Oregon), use us-west-2
  For eu-west-1 (Ireland), use EU
  For ap-southeast-1 (Singapore), use ap-southeast-1
  For ap-northeast-1 (Tokyo), use ap-northeast-1
  For sa-east-1 (Sao Paulo), use sa-east-1

- nuxeo.s3storage.cachesize : size of the local cache (default is 100MB).
- nuxeo.s3storage.bucket_prefix : bucket prefix
- nuxeo.s3storage.pathstyleaccess : if `true`, configures the client to use path-style access for all requests (default is `false`)

# Crypto parameters

With S3, you have the option to store your data encrypted.
Note that the local cache will *NOT* be encrypted.

The S3 binary manager can use a keystore containing a keypair, but there are
a few caveats to be aware of :

- The Sun/Oracle JDK doesn't always allow the AES256 cipher which the AWS SDK
  uses internally.
  Depending on the US export restrictions for your country, you may be able to
  modify your JDK to use AES256 by installing the "Java Cryptography Extension
  Unlimited Strength Jurisdiction Policy Files". See the following link to
  download the files and installation instructions:
  http://www.oracle.com/technetwork/java/javase/downloads/index.html

- Don't forget to specify the key algorithm if you create your keypair with the
  "keytool" command, as this won't work with the default (DSA).
  The S3 Binary Manager has been tested with a keystore generated with this
  command :
  keytool -genkeypair -keystore </path/to/keystore/file> -alias <key alias>
      -storepass <keystore password> -keypass <key password>
      -dname <key distinguished name> -keyalg RSA

With all that preceded in mind, here are the crypto options (they are all
mandatory once you specify a keystore) :

- nuxeo.s3storage.crypt.keystore.file : the absolute path to the keystore file
- nuxeo.s3storage.crypt.keystore.password : the keystore password
- nuxeo.s3storage.crypt.key.alias = the key alias
- nuxeo.s3storage.crypt.key.password = the key password

# Server-side encryption

S3 allows you to use server-side encryption to encrypt data at rest with keys managed
by S3 itself. To activate-this mode, use:

- nuxeo.s3storage.crypt.serverside = true

See http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingServerSideEncryption.html for more.

If you want to use Server-Side Encryption with AWS KMS–Managed Keys, specify your key id with:

- nuxeo.s3storage.crypt.kms.key = your-key-id

See https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingKMSEncryption.html for more.

# Enable CloudFront Direct Download

Please, read carefully the [CloudFront documentation](https://aws.amazon.com/fr/documentation/cloudfront/) to understand how you bind a CloudFront distribution to a S3 bucket.
After you created a CloudFront distribution domain bound to your S3 repository. Accessing your objects is not restricted per default. 

You have to enable the `restriction viewer access` (Look at [Serving Private Content throught CloudFront](http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/PrivateContent.html)), then each distribution URL must be signed to access the target object.

You have to set the `Query String Forwarding and Caching` on `all parameters` (Look at [Configuring CloudFront to Cache Based on Query String Parameters](http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/QueryStringParameters.html)) in order to correctly forward `Content Disposition` parameters to get a response with a correct filename. 

## Mandatory Parameters
S3 parameters (except `nuxeo.core.binarymanager`) are mandatory, additionally CloudFront requires some new ones:

 - `nuxeo.core.binarymanager=org.nuxeo.ecm.core.storage.sql.CloudFrontBinaryManager`
 - `nuxeo.s3storage.cloudfront.privKey`: the absolute path of the private key file (`.pem` or `.der`). Read: [Creating CloudFront Key Pairs for Your Trusted Signers](http://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-trusted-signers.html#private-content-creating-cloudfront-key-pairs) 
 - `nuxeo.s3storage.cloudfront.privKeyId`: the private key id. 
 - `nuxeo.s3storage.cloudfront.distribDomain`: the distribution domain name.
 - `nuxeo.s3storage.cloudfront.protocol`: the prefered protocol (default `HTTPS`)
 - `nuxeo.s3storage.cloudfront.fix.encoding`: Enable a workaround to fix an error on CloudFront side (default `false`)

# Building

    mvn clean install

## Deploying

Install [the Amazon S3 Online Storage Marketplace Package](https://connect.nuxeo.com/nuxeo/site/marketplace/package/amazon-s3-online-storage).
Or manually copy the built artifacts into `$NUXEO_HOME/templates/custom/bundles/` and activate the "custom" template.

# About Nuxeo

Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
