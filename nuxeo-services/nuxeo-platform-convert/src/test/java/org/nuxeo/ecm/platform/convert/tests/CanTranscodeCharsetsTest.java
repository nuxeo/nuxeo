/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.platform.convert.tests;

import java.io.IOException;
import java.io.InputStream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.convert.plugins.UTF8CharsetConverter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class CanTranscodeCharsetsTest {

    @Test
    public void transcodeLatin1() throws IOException {
        Blob blob;
        try (InputStream in = CanTranscodeCharsetsTest.class.getResource("/latin1.txt").openStream()) {
            Blob latin1Blob = Blobs.createBlob(in, "text/plain");
            BlobHolder holder = new SimpleBlobHolder(latin1Blob);
            UTF8CharsetConverter encoder = new UTF8CharsetConverter();
            blob = encoder.convert(holder, null).getBlob();
        }
        Assert.assertThat(blob.getMimeType(), Matchers.is("text/plain"));
        Assert.assertThat(blob.getEncoding(), Matchers.is("UTF-8"));
        String content = blob.getString();
        Assert.assertThat(content, Matchers.is("Test de prévisualisation avant rattachement"));
    }
}
