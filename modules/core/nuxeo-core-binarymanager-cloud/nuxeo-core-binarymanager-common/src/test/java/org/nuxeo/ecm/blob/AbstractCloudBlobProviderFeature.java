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
package org.nuxeo.ecm.blob;

import static org.apache.commons.lang3.ObjectUtils.getFirstNonNull;

import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 2023.5
 */
public class AbstractCloudBlobProviderFeature implements RunnerFeature {

    protected static void clearBlobStore(String blobProviderId) {
        var blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blobProviderId);
        var blobStore = ((BlobStoreBlobProvider) blobProvider).store;
        blobStore.clear();
    }

    protected static void clearBlobStores() {
        clearBlobStore("test");
        clearBlobStore("other");
    }

    protected static String configureProperty(String key,
            @SuppressWarnings("unchecked") Supplier<String>... suppliers) {
        String value = getFirstNonNull(suppliers);
        if (value != null) {
            Framework.getProperties().setProperty(key, value);
        }
        return value;
    }

    public static String getUniqueBucketPrefix(String prefix) {
        long timestamp = System.nanoTime();
        return String.format("%s-%s/", prefix, timestamp);
    }

    protected static Supplier<String> sysEnv(String key) {
        return () -> StringUtils.trimToNull(System.getenv(key));
    }

    protected static Supplier<String> sysProp(String key) {
        return () -> StringUtils.trimToNull(System.getProperty(key));
    }

    protected static Supplier<String> unique(String prefix) {
        return () -> prefix == null ? null : getUniqueBucketPrefix(prefix);
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        clearBlobStores();
    }

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        clearBlobStores();
    }
}
