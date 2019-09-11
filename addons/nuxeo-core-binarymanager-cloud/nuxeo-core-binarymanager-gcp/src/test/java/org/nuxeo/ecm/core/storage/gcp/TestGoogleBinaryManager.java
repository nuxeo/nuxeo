/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.core.storage.gcp;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.blob.AbstractTestCloudBinaryManager;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.cloud.storage.StorageException;

/**
 * WARNING: You must pass those variables to your test configuration:
 * <p/>
 * <pre>
 *   -Dnuxeo.gcp.project: GCP Project ID
 *   -Dnuxeo.gcp.storage.bucket: GCP Bucket ID
 *   -Dnuxeo.gcp.credentials: See following options
 * </pre>
 * <ul>
 * <li>nuxeo.gcp.credentials=/path/to/file.json</li>
 * <li>nuxeo.gcp.credentials=file.json (located in nxserver/config)</li>
 * <li>If nothing is set, Nuxeo will look into 'gcp-credentials.json' file by default (located in nxserver/config)</li>
 * </ul>
 * <p/>
 * Don't set any bucket prefix (nuxeo.gcp.storage.bucket_prefix) as the unit tests don't check it.
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestGoogleBinaryManager extends AbstractTestCloudBinaryManager<GoogleStorageBinaryManager> {

    protected final static List<String> PARAMETERS = Arrays.asList(GoogleStorageBinaryManager.BUCKET_NAME_PROPERTY,
            GoogleStorageBinaryManager.PROJECT_ID_PROPERTY, GoogleStorageBinaryManager.GOOGLE_APPLICATION_CREDENTIALS);

    protected static Map<String, String> properties = new HashMap<>();

    @BeforeClass
    public static void initialize() {
        AbstractCloudBinaryManager bm = new GoogleStorageBinaryManager();
        PARAMETERS.forEach(s -> properties.put(s, Framework.getProperty(bm.getSystemPropertyName(s))));
        // Ensure mandatory parameters are set
        PARAMETERS.forEach(s -> assumeFalse(isBlank(properties.get(s))));
    }

    @AfterClass
    public static void afterClass() {
        // Cleanup keys
        Properties props = Framework.getProperties();
        PARAMETERS.forEach(props::remove);
    }

    @Test
    public void iCanCreateAndDeleteBuckets() {
        assertThat(binaryManager.getOrCreateBucket(binaryManager.bucketName)).isNotNull();
        assertThat(binaryManager.deleteBucket(binaryManager.bucketName)).isTrue();
    }

    @Override
    protected GoogleStorageBinaryManager getBinaryManager() throws IOException {
        GoogleStorageBinaryManager binaryManager = new GoogleStorageBinaryManager();
        binaryManager.initialize("gcptest", properties);
        return binaryManager;
    }

    @Override
    protected Set<String> listObjects() {
        Set<String> digests = new HashSet<>();
        try {
            binaryManager.storage.list(binaryManager.bucketName).iterateAll().forEach(blob -> {
                if (blob != null) {
                    digests.add(binaryManager.bucketPrefix + blob.getName());
                }
            });
            return digests;
        } catch (StorageException e) {
            if (e.getMessage().equals("Not Found")) {
                return digests;
            }
            throw new NuxeoException(e);
        }
    }

}
