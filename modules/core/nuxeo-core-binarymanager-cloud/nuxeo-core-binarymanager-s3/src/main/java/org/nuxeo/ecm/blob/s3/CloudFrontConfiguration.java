/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import org.nuxeo.ecm.blob.CloudBlobStoreConfiguration;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.auth.PEM;
import com.amazonaws.auth.RSA;
import com.amazonaws.services.cloudfront.util.SignerUtils.Protocol;
import com.amazonaws.util.IOUtils;

/**
 * CloudFront configuration.
 *
 * @since 11.1
 */
public class CloudFrontConfiguration extends CloudBlobStoreConfiguration {

    public static final String CLOUDFRONT_ENABLED_PROPERTY = "cloudfront.enabled";

    public static final String CLOUDFRONT_PRIVATE_KEY_PROPERTY = "cloudfront.privKey";

    public static final String CLOUDFRONT_PRIVATE_KEY_ID_PROPERTY = "cloudfront.privKeyId";

    public static final String CLOUDFRONT_DISTRIBUTION_DOMAIN_PROPERTY = "cloudfront.distribDomain";

    public static final String CLOUDFRONT_PROTOCOL_PROPERTY = "cloudfront.protocol";

    // framework property for bug workaround
    public static final String CLOUDFRONT_ENABLE_ENCODING_FIX = "nuxeo.s3storage.cloudfront.fix.encoding";

    public final boolean enabled;

    public final String distributionDomain;

    public final Protocol protocol;

    public final PrivateKey privateKey;

    public final String keyPairId;

    public final boolean fixEncoding;

    public CloudFrontConfiguration(String systemPropertyPrefix, Map<String, String> properties) throws IOException {
        super(systemPropertyPrefix, properties);
        enabled = getBooleanProperty(CLOUDFRONT_ENABLED_PROPERTY);
        if (enabled) {
            protocol = Protocol.valueOf(getProperty(CLOUDFRONT_PROTOCOL_PROPERTY, "https"));
            distributionDomain = getProperty(CLOUDFRONT_DISTRIBUTION_DOMAIN_PROPERTY);
            privateKey = getPrivateKey(getProperty(CLOUDFRONT_PRIVATE_KEY_PROPERTY));
            keyPairId = getProperty(CLOUDFRONT_PRIVATE_KEY_ID_PROPERTY);
            // framework property for CloudFront bug workaround
            fixEncoding = Framework.isBooleanPropertyTrue(CLOUDFRONT_ENABLE_ENCODING_FIX);
        } else {
            protocol = null;
            distributionDomain = null;
            privateKey = null;
            keyPairId = null;
            fixEncoding = false;
        }
    }

    protected PrivateKey getPrivateKey(String path) throws IOException {
        if (path == null) {
            return null;
        }
        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            if (path.toLowerCase().endsWith(".pem")) {
                return PEM.readPrivateKey(in);
            }
            if (path.toLowerCase().endsWith(".der")) {
                return RSA.privateKeyFromPKCS8(IOUtils.toByteArray(in));
            }
            throw new IllegalArgumentException("Unsupported file type");
        } catch (InvalidKeySpecException | IllegalArgumentException e) {
            throw new IOException("Invalid CloudFront private key: " + path, e);
        }
    }

}
