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

import static org.nuxeo.runtime.api.Framework.isBooleanPropertyTrue;

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
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;
import com.amazonaws.util.IOUtils;

/**
 * @since 9.1
 */
public class CloudFrontBinaryManager extends S3BinaryManager {

    private static final String BASE_PROP = "cloudfront.";

    public static final String PRIVATE_KEY_PROPERTY = BASE_PROP + "privKey";

    public static final String PRIVATE_KEY_ID_PROPERTY = BASE_PROP + "privKeyId";

    public static final String DISTRIB_DOMAIN_PROPERTY = BASE_PROP + "distribDomain";

    public static final String PROTOCOL_PROPERTY = BASE_PROP + "protocol";

    public static final String ENABLE_CF_ENCODING_FIX = "nuxeo.s3storage.cloudfront.fix.encoding";

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
        try {
            URIBuilder uriBuilder = new URIBuilder(buildResourcePath(bucketNamePrefix + digest));
            if (blob != null) {
                uriBuilder.addParameter("response-content-type", getContentTypeHeader(blob));
                uriBuilder.addParameter("response-content-disposition",
                        getContentDispositionHeader(blob, servletRequest));
            }

            if (isBooleanPropertyTrue(ENABLE_CF_ENCODING_FIX)) {
                String trimmedChars = " ";
                uriBuilder.getQueryParams().stream().filter(s -> s.getValue().contains(trimmedChars)).forEach(
                        s -> uriBuilder.setParameter(s.getName(), s.getValue().replace(trimmedChars, "")));
            }

            URI uri = uriBuilder.build();
            if (privKey == null) {
                return uri;
            }

            Date expiration = new Date();
            expiration.setTime(expiration.getTime() + directDownloadExpire * 1000);

            String signedURL = CloudFrontUrlSigner.getSignedURLWithCannedPolicy(uri.toString(), privKeyId, privKey,
                    expiration);
            return new URI(signedURL);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private String buildResourcePath(String s3ObjectKey) {
        return protocol != Protocol.http && protocol != Protocol.https
                ? s3ObjectKey : protocol + "://" + distributionDomain + "/" + s3ObjectKey;
    }

    private static PrivateKey loadPrivateKey(String privateKeyPath) throws InvalidKeySpecException, IOException {
        if (privateKeyPath == null) {
            return null;
        }

        try (FileInputStream is = new FileInputStream(new File(privateKeyPath))) {
            if (privateKeyPath.toLowerCase().endsWith(".pem")) {
                return PEM.readPrivateKey(is);
            }

            if (privateKeyPath.toLowerCase().endsWith(".der")) {
                return RSA.privateKeyFromPKCS8(IOUtils.toByteArray(is));
            }

            throw new AmazonClientException("Unsupported file type for private key");
        }
    }
}
