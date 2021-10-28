/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.blob.s3;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_SESSION_TOKEN_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.URLStreamRef;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.osgi.framework.Bundle;

/**
 * This feature deploys two {@link S3BlobProvider} configured through environment variables and system properties.
 * <p>
 * The blob provider will load its configuration as below:
 * <ul>
 * <li>loads the property from the contribution
 * <li>loads the property from the system properties having {@link S3BlobStoreConfiguration#SYSTEM_PROPERTY_PREFIX} as
 * prefix
 * </ul>
 * This feature will plug into this behavior and do:
 * <ul>
 * <li>load environment variables if any
 * <li>load test system properties if any
 * <li>put these properties into the contribution
 * </ul>
 * By default, the two {@link S3BlobProvider} will have the same {@code awsid}, {@code awssecret}, {@code awstoken}, and
 * {@code bucket}. The blob provider {@code test} will use {@code provider-test/} as {@code bucket_prefix}, respectively
 * {@code provider-other/} for the blob provider {@code other}.
 * <p>
 * If a test needs a specific blob provider settings, it can deploy a partial contribution with these settings only, the
 * descriptor merge behavior will do the necessary.
 * <p>
 * The buckets will be cleared before and after each test execution.
 * <p>
 * Additionally, this feature will contribute the {@link #S3_DOC_TYPE} document type with {@code s3-test} and
 * {@code s3-other} file schemas both dispatched respectively in {@code test} and {@code other} blob providers.
 *
 * @since 2021.11
 */
@Features(BlobManagerFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3")
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests")
public class S3BlobProviderFeature implements RunnerFeature {

    public static String S3_DOC_TYPE = "FileS3";

    public static final String PREFIX_TEST = "nuxeo.test.s3storage.";

    public static final String PREFIX_PROVIDER_TEST = PREFIX_TEST + "provider.test.";

    public static final String PREFIX_PROVIDER_OTHER = PREFIX_TEST + "provider.other.";

    // ---------------------------------
    // properties for all blob providers
    // ---------------------------------

    public static final String AWS_ID = PREFIX_TEST + S3BlobStoreConfiguration.AWS_ID_PROPERTY;

    public static final String AWS_SECRET = PREFIX_TEST + S3BlobStoreConfiguration.AWS_SECRET_PROPERTY;

    public static final String AWS_SESSION_TOKEN = PREFIX_TEST + S3BlobStoreConfiguration.AWS_SESSION_TOKEN_PROPERTY;

    public static final String BUCKET_REGION = PREFIX_TEST + S3BlobStoreConfiguration.BUCKET_REGION_PROPERTY;

    public static final String BUCKET = PREFIX_TEST + S3BlobStoreConfiguration.BUCKET_NAME_PROPERTY;

    // ----------------------------
    // properties by blob providers
    // ----------------------------

    public static final String PROVIDER_TEST_BUCKET = PREFIX_PROVIDER_TEST
            + S3BlobStoreConfiguration.BUCKET_NAME_PROPERTY;

    public static final String PROVIDER_TEST_BUCKET_PREFIX = PREFIX_PROVIDER_TEST
            + S3BlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;

    public static final String DEFAULT_PROVIDER_TEST_BUCKET_PREFIX = "provider-test/";

    public static final String PROVIDER_OTHER_BUCKET = PREFIX_PROVIDER_OTHER
            + S3BlobStoreConfiguration.BUCKET_NAME_PROPERTY;

    public static final String PROVIDER_OTHER_BUCKET_PREFIX = PREFIX_PROVIDER_OTHER
            + S3BlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;

    public static final String DEFAULT_PROVIDER_OTHER_BUCKET_PREFIX = "provider-other/";

    @Override
    @SuppressWarnings("unchecked")
    public void start(FeaturesRunner runner) {
        // configure global blob provider properties
        String awsId = configureProperty(AWS_ID, sysEnv(ACCESS_KEY_ENV_VAR), sysEnv(ALTERNATE_ACCESS_KEY_ENV_VAR),
                sysProp(AWS_ID));
        String awsSecret = configureProperty(AWS_SECRET, sysEnv(SECRET_KEY_ENV_VAR),
                sysEnv(ALTERNATE_SECRET_KEY_ENV_VAR), sysProp(AWS_SECRET));
        configureProperty(AWS_SESSION_TOKEN, sysEnv(AWS_SESSION_TOKEN_ENV_VAR), sysProp(AWS_SESSION_TOKEN));
        configureProperty(BUCKET_REGION, sysEnv(AWS_REGION_ENV_VAR), sysProp(BUCKET_REGION));
        // configure specific blob provider properties
        configureProperty(PROVIDER_TEST_BUCKET, sysProp(PROVIDER_TEST_BUCKET), sysProp(BUCKET));
        configureProperty(PROVIDER_TEST_BUCKET_PREFIX, sysProp(PROVIDER_TEST_BUCKET_PREFIX),
                constant(DEFAULT_PROVIDER_TEST_BUCKET_PREFIX));
        configureProperty(PROVIDER_OTHER_BUCKET, sysProp(PROVIDER_OTHER_BUCKET), sysProp(PROVIDER_TEST_BUCKET),
                sysProp(BUCKET));
        configureProperty(PROVIDER_OTHER_BUCKET_PREFIX, sysProp(PROVIDER_OTHER_BUCKET_PREFIX),
                constant(DEFAULT_PROVIDER_OTHER_BUCKET_PREFIX));
        // check if tests can run
        assumeTrue("AWS Credentials are missing in test configuration", isNoneBlank(awsId, awsSecret));
        // deploy the test bundle after the properties have been set
        try {
            RuntimeHarness harness = runner.getFeature(RuntimeFeature.class).getHarness();
            Bundle bundle = harness.getOSGiAdapter()
                                   .getRegistry()
                                   .getBundle("org.nuxeo.ecm.core.storage.binarymanager.s3.tests");
            URL url = bundle.getEntry("OSGI-INF/test-feature-blob-provider-s3.xml");
            harness.getContext().deploy(new URLStreamRef(url));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected Supplier<String> sysEnv(String key) {
        return () -> StringUtils.trimToNull(System.getenv(key));
    }

    protected Supplier<String> sysProp(String key) {
        return () -> StringUtils.trimToNull(System.getProperty(key));
    }

    protected Supplier<String> constant(String value) {
        return () -> value;
    }

    protected String configureProperty(String key, Supplier<String>... suppliers) {
        String value = getFirstNonNull(suppliers);
        if (value != null) {
            Framework.getProperties().setProperty(key, value);
        }
        return value;
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        clearBlobStore("test");
        clearBlobStore("other");
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        clearBlobStore("test");
        clearBlobStore("other");
    }

    protected void clearBlobStore(String blobProviderId) {
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blobProviderId);
        BlobStore blobStore = ((BlobStoreBlobProvider) blobProvider).store;
        blobStore.clear();
    }

    protected static <T> T getFirstNonNull(final Supplier<T>... suppliers) {
        if (suppliers != null) {
            for (final Supplier<T> supplier : suppliers) {
                if (supplier != null) {
                    final T value = supplier.get();
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }
}
