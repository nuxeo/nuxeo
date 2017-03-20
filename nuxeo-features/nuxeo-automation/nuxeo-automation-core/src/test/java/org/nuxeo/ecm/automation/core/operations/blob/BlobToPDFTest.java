/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.io" })
public class BlobToPDFTest {

    @Test
    public void testReplaceURLsByAbsolutePaths() throws IOException {
        String filename = "plouf.png";
        FileBlob imageBlob = new FileBlob("image");
        imageBlob.setFilename(filename);
        imageBlob.setMimeType("image/png");

        String url = "http://localhost:8080/nuxeo/nxfile/default/3727ef6b-cf8c/files:files/0/file/" + filename;
        Blob blob = new StringBlob("<h1>Hello World</><img src=\"" + url + "\" />");
        blob.setMimeType("text/html");

        DownloadService downloadService = mock(DownloadService.class);
        doReturn(imageBlob).when(downloadService).resolveBlobFromDownloadUrl(url);

        Path tempDirectory = Framework.createTempDirectory("blobs");
        Blob newBlob = BlobToPDF.replaceURLsByAbsolutePaths(blob, tempDirectory, downloadService);
        String absolutePath = tempDirectory.toString() + "/" + filename;
        assertEquals("<h1>Hello World</><img src=\"" + absolutePath + "\" />", newBlob.getString());
        FileUtils.deleteQuietly(tempDirectory.toFile());
    }

}
