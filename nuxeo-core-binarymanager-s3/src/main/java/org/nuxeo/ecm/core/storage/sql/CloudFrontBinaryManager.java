/*
 * (C) Copyright 2011-2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Arnaud Kervern
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.utils.URIBuilder;
import org.nuxeo.ecm.core.blob.ManagedBlob;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.PEM;
import com.amazonaws.auth.RSA;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner.Protocol;
import com.amazonaws.util.IOUtils;

public class CloudFrontBinaryManager extends S3BinaryManager {

    private static final String BASE_PROP = "cloudfront.";

    public static final String PRIVATE_KEY_PROPERTY = BASE_PROP + "privKey";

    public static final String PRIVATE_KEY_ID_PROPERTY = BASE_PROP + "privKeyId";

    public static final String DISTRIB_DOMAIN_PROPERTY = BASE_PROP + "distribDomain";

    public static final String PROTOCOL_PROPERTY = BASE_PROP + "protocol";

    protected String distributionDomain;

    protected Protocol protocol;

    protected PrivateKey privKey;

    protected String privKeyId;

    @Override
    protected void setupCloudClient() throws IOException {
        super.setupCloudClient();

        protocol = Protocol.valueOf(getProperty(PROTOCOL_PROPERTY, "https"));
        distributionDomain = getProperty(DISTRIB_DOMAIN_PROPERTY);

        try {
            String privateKeyPath = getProperty(PRIVATE_KEY_PROPERTY);
            privKey = loadPrivateKey(privateKeyPath);
            privKeyId = getProperty(PRIVATE_KEY_ID_PROPERTY);
        } catch (InvalidKeySpecException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected URI getRemoteUri(String digest, ManagedBlob blob, HttpServletRequest servletRequest) throws IOException {
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + directDownloadExpire * 1000);

        try {
            URIBuilder uriBuilder = new URIBuilder(buildResourcePath(bucketNamePrefix + digest));
            if (blob != null) {
                uriBuilder.addParameter("response-content-type", getContentTypeHeader(blob));
                uriBuilder.addParameter("response-content-disposition",
                        getContentDispositionHeader(blob, servletRequest));
            }

            String uri;
            if (privKey != null) {
                uri = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(uriBuilder.build().toString(), privKeyId,
                        privKey, expiration);
            } else {
                uri = uriBuilder.build().toString();
            }

            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    protected String buildResourcePath(String s3ObjectKey) {
        return protocol != CloudFrontUrlSigner.Protocol.http && protocol != CloudFrontUrlSigner.Protocol.https
                ? s3ObjectKey : protocol + "://" + distributionDomain + "/" + s3ObjectKey;
    }

    /**
     * Originally from CloudFrontUrlSigner.loadPrivateKey()
     */
    static PrivateKey loadPrivateKey(String privateKeyPath) throws InvalidKeySpecException, IOException {
        if (privateKeyPath == null) {
            return null;
        }

        File privateKeyFile = new File(privateKeyPath);

        FileInputStream is;
        PrivateKey var2;
        if (privateKeyFile.getAbsolutePath().toLowerCase().endsWith(".pem")) {
            is = new FileInputStream(privateKeyFile);

            try {
                var2 = PEM.readPrivateKey(is);
            } finally {
                try {
                    is.close();
                } catch (IOException var19) {
                    ;
                }

            }

            return var2;
        } else if (privateKeyFile.getAbsolutePath().toLowerCase().endsWith(".der")) {
            is = new FileInputStream(privateKeyFile);

            try {
                var2 = RSA.privateKeyFromPKCS8(IOUtils.toByteArray(is));
            } finally {
                try {
                    is.close();
                } catch (IOException var20) {
                    ;
                }

            }

            return var2;
        } else {
            throw new AmazonClientException("Unsupported file type for private key");
        }
    }
}
