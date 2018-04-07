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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.storage.sql.AmazonS3Client.AWS_ID_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.AmazonS3Client.AWS_SECRET_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.AmazonS3Client.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.AmazonS3Client.BUCKET_PREFIX_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.CloudFrontBinaryManager.DISTRIB_DOMAIN_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.CloudFrontBinaryManager.PRIVATE_KEY_ID_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.CloudFrontBinaryManager.PRIVATE_KEY_PROPERTY;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Test ignored as it requires lots of file and tokens. Manually executed
 */
@Ignore("It requires lots of file and tokens. Only manually executed")
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestCloudFrontRedirect extends AbstractS3BinaryTest<CloudFrontBinaryManager> {
    @BeforeClass
    public static void beforeClass() {
        PROPERTIES = new HashMap<>();
        // NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!!
        // ********** NEVER COMMIT THE SECRET KEYS !!! **********
        String bucketName = "CHANGETHIS";
        String idKey = "CHANGETHIS";
        String secretKey = "CHANGETHIS";

        String privateKeyPath = "CHANGETHIS";
        String privateKeyId = "CHANGETHIS";
        String distribution = "CHANGETHIS";
        // ********** NEVER COMMIT THE SECRET KEYS !!! **********

        PROPERTIES.put(BUCKET_NAME_PROPERTY, bucketName);
        PROPERTIES.put(BUCKET_PREFIX_PROPERTY, "testfolder/");
        PROPERTIES.put(AWS_ID_PROPERTY, idKey);
        PROPERTIES.put(AWS_SECRET_PROPERTY, secretKey);
        PROPERTIES.put(DISTRIB_DOMAIN_PROPERTY, distribution);
        PROPERTIES.put(PRIVATE_KEY_PROPERTY, privateKeyPath);
        PROPERTIES.put(PRIVATE_KEY_ID_PROPERTY, privateKeyId);
        boolean disabled = bucketName.equals("CHANGETHIS");
        assumeTrue("No AWS credentials configured", !disabled);
    }

    @Before
    public void tearUp() throws IOException {
        removeObjects();
    }

    @Test
    public void testRemoteUri() throws Exception {
        testStoreFile();
        String key = listObjects().stream().findAny().orElseThrow(Exception::new);

        URI remoteUri = getBinaryManager().getRemoteUri(key, null, null);
        assertNotNull(remoteUri);
        System.out.println(String.format("curl -v \"%s\"", remoteUri));
    }

    @Override
    protected CloudFrontBinaryManager getBinaryManager() throws IOException {
        CloudFrontBinaryManager binaryManager = new CloudFrontBinaryManager();
        binaryManager.initialize("repo", PROPERTIES);
        return binaryManager;
    }
}
