/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Remi Cattiau
 *     Florent Guillaume
 */
package org.nuxeo.runtime.aws;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SESSION_TOKEN_SYSTEM_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.AwsEnvVarOverrideRegionProvider;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.aws")
public class TestAWSConfigurationService {

    @Inject
    protected AWSConfigurationService service;

    protected static void setSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testServiceCredentials() {
        AWSCredentials credentials = service.getAWSCredentials();
        assertCredentials(credentials, "XML_ACCESS_KEY_ID", "XML_SECRET_KEY", "XML_SESSION_TOKEN");
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testNuxeoCredentialsProvider() {
        AWSCredentials credentials = NuxeoAWSCredentialsProvider.getInstance().getCredentials();
        assertCredentials(credentials, "XML_ACCESS_KEY_ID", "XML_SECRET_KEY", "XML_SESSION_TOKEN");
    }

    @Test
    public void testServiceCredentialsWithoutNuxeoConfiguration() {
        assertNull(service.getAWSCredentials());
    }

    @Test
    public void testNuxeoCredentialsProviderWithoutNuxeoConfiguration() {
        try {
            assumeTrue("Cannot run if AWS env vars are already set",
                    new EnvironmentVariableCredentialsProvider().getCredentials() == null);
        } catch (SdkClientException e) {
            // ok, no env vars set
        }

        // use system prop config from default AWS chain
        String oldAccessKeyId = System.getProperty(ACCESS_KEY_SYSTEM_PROPERTY);
        String oldSecretKey = System.getProperty(SECRET_KEY_SYSTEM_PROPERTY);
        String oldSessionToken = System.getProperty(SESSION_TOKEN_SYSTEM_PROPERTY);
        try {
            setSystemProperty(ACCESS_KEY_SYSTEM_PROPERTY, "SYSPROP_ACCESS_KEY_ID");
            setSystemProperty(SECRET_KEY_SYSTEM_PROPERTY, "SYSPROP_SECRET_KEY");
            setSystemProperty(SESSION_TOKEN_SYSTEM_PROPERTY, "SYSPROP_SESSION_TOKEN");
            AWSCredentials credentials = NuxeoAWSCredentialsProvider.getInstance().getCredentials();
            assertCredentials(credentials, "SYSPROP_ACCESS_KEY_ID", "SYSPROP_SECRET_KEY", "SYSPROP_SESSION_TOKEN");
        } finally {
            setSystemProperty(ACCESS_KEY_SYSTEM_PROPERTY, oldAccessKeyId);
            setSystemProperty(SECRET_KEY_SYSTEM_PROPERTY, oldSecretKey);
            setSystemProperty(SESSION_TOKEN_SYSTEM_PROPERTY, oldSessionToken);
        }
    }

    protected static void assertCredentials(AWSCredentials credentials, String accessKeyId, String secretKey,
            String sessionToken) {
        assertNotNull(credentials);
        assertEquals(accessKeyId, credentials.getAWSAccessKeyId());
        assertEquals(secretKey, credentials.getAWSSecretKey());
        if (sessionToken != null) {
            assertTrue(credentials instanceof AWSSessionCredentials);
            assertEquals(sessionToken, ((AWSSessionCredentials) credentials).getSessionToken());
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testServiceRegion() {
        String region = service.getAWSRegion();
        assertEquals("XML_REGION", region);
    }

    @Test
    @Deploy("org.nuxeo.runtime.aws:OSGI-INF/test-aws-config.xml")
    public void testNuxeoRegionProvider() {
        String region = NuxeoAWSRegionProvider.getInstance().getRegion();
        assertEquals("XML_REGION", region);
    }

    @Test
    public void testServiceRegionWithoutNuxeoConfiguration() {
        assertNull(service.getAWSRegion());
    }

    @Test
    public void testNuxeoRegionProviderWithoutNuxeoConfiguration() {
        assumeTrue("Cannot run if AWS env vars are already set",
                new AwsEnvVarOverrideRegionProvider().getRegion() == null);

        // use system prop config from default AWS chain
        String oldRegion = System.getProperty(AWS_REGION_SYSTEM_PROPERTY);
        try {
            setSystemProperty(AWS_REGION_SYSTEM_PROPERTY, "SYSPROP_REGION");
            String region = NuxeoAWSRegionProvider.getInstance().getRegion();
            assertEquals("SYSPROP_REGION", region);
        } finally {
            setSystemProperty(AWS_REGION_SYSTEM_PROPERTY, oldRegion);
        }
    }

}
