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
 *     Thomas Roger
 */

package org.nuxeo.ecm.diff.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.convert:OSGI-INF/convert-service-contrib.xml")
@Deploy("org.nuxeo.diff.content")
public class TestContentDiffConverters {

    @Inject
    protected ConversionService conversionService;

    @Test
    public void testContentDiffHTMLConverterInfiniteLoop() {
        // no specific converter registered for 'foo/bar' => 'text/html'
        // will fallback on contentDiffHtmlConverter that handles '*' => 'text/html'
        // => infinite loop
        Blob blob = Blobs.createBlob("foo", "foo/bar");
        BlobHolder bh = new SimpleBlobHolder(blob);
        try {
            conversionService.convert("contentDiffHtmlConverter", bh, null);
            fail();
        } catch (ConverterNotRegistered e) {
            assertEquals("Converter for sourceMimeType = foo/bar, destinationMimeType = text/html is not registered",
                    e.getMessage());
        }
    }

    @Test
    public void testContentDiffTextConverterInfiniteLoop() {
        // no specific converter registered for 'foo/bar' => 'text/plain'
        // will fallback on contentDiffTextConverter that handles '*' => 'text/plain'
        // => infinite loop
        Blob blob = Blobs.createBlob("foo", "foo/bar");
        BlobHolder bh = new SimpleBlobHolder(blob);
        try {
            conversionService.convert("contentDiffTextConverter", bh, null);
            fail();
        } catch (ConverterNotRegistered e) {
            assertEquals("Converter for sourceMimeType = foo/bar, destinationMimeType = text/plain is not registered",
                    e.getMessage());
        }
    }
}
