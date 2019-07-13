/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Thibaud Arguillere
 */
package org.nuxeo.binary.metadata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@Deploy("org.nuxeo.binary.metadata:binary-metadata-readonly-test.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = BinaryMetadataServerInit.class)
public class TestBinaryMetadataDataMappingReadOnly extends BaseBinaryMetadataTest {

    @Test
    public void itShouldHandleReadonlyMappingAttribute() {
        // Get the document with PDF attached.
        DocumentModel pdfDoc = BinaryMetadataServerInit.getFile(1, session);

        // Copy into the document according to metadata mapping contribution.
        binaryMetadataService.writeMetadata(pdfDoc);
        session.saveDocument(pdfDoc);

        // Check if the document has been overwritten by binary metadata.
        pdfDoc = BinaryMetadataServerInit.getFile(1, session);
        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:source"));
        
        // Modify values in the document
        pdfDoc.setPropertyValue("dc:title", "New Title");
        pdfDoc.setPropertyValue("dc:source", "New Source");
        pdfDoc = session.saveDocument(pdfDoc);

        // According to binary-metadata-readonly-test.xml, mapping to dc:title was read only
        pdfDoc = session.getDocument(pdfDoc.getRef());
        Blob blob = (Blob) pdfDoc.getPropertyValue("file:content");
        Map<String, Object> blobProperties = binaryMetadataService.readMetadata(blob, true);
        assertNotNull(blobProperties);
        assertEquals("en-US", blobProperties.get("Language").toString());
        assertEquals("New Source", blobProperties.get("Producer").toString());
        
    }
}
