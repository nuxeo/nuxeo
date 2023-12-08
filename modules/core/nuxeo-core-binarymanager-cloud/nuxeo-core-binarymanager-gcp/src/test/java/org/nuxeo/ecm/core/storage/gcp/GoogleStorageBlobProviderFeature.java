/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.storage.gcp;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.blob.AbstractCloudBlobProviderFeature;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.osgi.framework.Bundle;

/**
 * This feature deploys two {@link GoogleStorageBlobProvider} configured through environment variables and system
 * properties.
 * <p>
 * The blob provider will load its configuration as below:
 * <ul>
 * <li>loads the property from the contribution
 * <li>loads the property from the system properties having
 * {@link GoogleStorageBlobStoreConfiguration#SYSTEM_PROPERTY_PREFIX} as prefix
 * </ul>
 * This feature will plug into this behavior and do:
 * <ul>
 * <li>load environment variables if any
 * <li>load test system properties if any
 * <li>put these properties into the contribution
 * </ul>
 * By default, the two {@link GoogleStorageBlobProvider} will have the same {@code projectId}, {@code credentials}, and
 * {@code bucket}. The blob provider {@code test} will use {@code provider-test-TIMESTAMP/} as default
 * {@code bucket_prefix}, respectively {@code provider-other-TIMESTAMP/} for the blob provider {@code other}.
 * <p>
 * If a test needs a specific blob provider settings, it can deploy a partial contribution with these settings only, the
 * descriptor merge behavior will do the necessary.
 * <p>
 * The buckets will be cleared before and after each test execution.
 *
 * @since 2023.5
 */
@Features(BlobManagerFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.gcp")
@Deploy("org.nuxeo.ecm.core.storage.gcp.tests")
public class GoogleStorageBlobProviderFeature extends AbstractCloudBlobProviderFeature {

    protected static final Logger log = LogManager.getLogger(GoogleStorageBlobProviderFeature.class);

    public static final String PREFIX_TEST = "nuxeo.test.gcp.";

    public static final String PREFIX_PROVIDER_TEST = PREFIX_TEST + "provider.test.";

    public static final String PREFIX_PROVIDER_OTHER = PREFIX_TEST + "provider.other.";

    // ---------------------------------
    // properties for all blob providers
    // ---------------------------------

    public static final String PROJECT_ID = PREFIX_TEST + GoogleStorageBlobStoreConfiguration.PROJECT_ID_PROPERTY;

    public static final String BUCKET = PREFIX_TEST + GoogleStorageBlobStoreConfiguration.BUCKET_NAME_PROPERTY;

    public static final String CREDENTIALS = PREFIX_TEST
            + GoogleStorageBlobStoreConfiguration.GOOGLE_APPLICATION_CREDENTIALS;

    // ---------------------------------
    // Env var alternatives
    // ---------------------------------
    public static final String PROJECT_ID_ENV_VAR = "GCP_PROJECT_ID";

    public static final String CREDENTIALS_PATH_ENV_VAR = "GCP_CREDENTIALS_PATH";

    // ----------------------------
    // properties by blob providers
    // ----------------------------
    public static final String PROVIDER_TEST_BUCKET = PREFIX_PROVIDER_TEST
            + GoogleStorageBlobStoreConfiguration.BUCKET_NAME_PROPERTY;

    public static final String PROVIDER_TEST_BUCKET_PREFIX = PREFIX_PROVIDER_TEST
            + GoogleStorageBlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;

    public static final String DEFAULT_PROVIDER_TEST_BUCKET_PREFIX = "provider-test";

    public static final String PROVIDER_OTHER_BUCKET = PREFIX_PROVIDER_OTHER
            + GoogleStorageBlobStoreConfiguration.BUCKET_NAME_PROPERTY;

    public static final String PROVIDER_OTHER_BUCKET_PREFIX = PREFIX_PROVIDER_OTHER
            + GoogleStorageBlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;

    public static final String DEFAULT_PROVIDER_OTHER_BUCKET_PREFIX = "provider-other";

    public static final String BUCKET_ENV_VAR = "GCP_BUCKET_NAME";

    public static final String BUCKET_PREFIX_ENV_VAR = "GCP_BUCKET_PREFIX";

    @SuppressWarnings("unchecked")
    @Override
    public void start(FeaturesRunner runner) {
        // configure global blob provider properties
        var projectId = configureProperty(PROJECT_ID, sysEnv(PROJECT_ID_ENV_VAR), sysProp(PROJECT_ID));
        var credentialPath = configureProperty(CREDENTIALS, sysEnv(CREDENTIALS_PATH_ENV_VAR), sysProp(CREDENTIALS));
        // configure specific blob provider properties
        var testBucket = configureProperty(PROVIDER_TEST_BUCKET, sysProp(PROVIDER_TEST_BUCKET), sysProp(BUCKET));
        configureProperty(PROVIDER_TEST_BUCKET_PREFIX, unique(sysProp(PROVIDER_TEST_BUCKET_PREFIX).get()),
                unique(DEFAULT_PROVIDER_TEST_BUCKET_PREFIX));
        var otherBucket = configureProperty(PROVIDER_OTHER_BUCKET, sysProp(PROVIDER_OTHER_BUCKET),
                sysProp(PROVIDER_TEST_BUCKET), sysProp(BUCKET));
        configureProperty(PROVIDER_OTHER_BUCKET_PREFIX, unique(sysProp(PROVIDER_OTHER_BUCKET_PREFIX).get()),
                unique(DEFAULT_PROVIDER_OTHER_BUCKET_PREFIX));
        // check if tests can run
        assumeTrue("GCP projectId, credentials and bucket are missing in test configuration",
                isNoneBlank(projectId, credentialPath, testBucket, otherBucket));
        // deploy the test bundle after the properties have been set
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            Bundle bundle = harness.getOSGiAdapter().getRegistry().getBundle("org.nuxeo.ecm.core.storage.gcp.tests");
            URL url = bundle.getEntry("OSGI-INF/test-google-storage-config.xml");
            harness.getContext().deploy(new URLStreamRef(url));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }
}
