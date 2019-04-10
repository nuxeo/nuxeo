/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.binary.metadata.internals.operations.ReadMetadataFromBinary;
import org.nuxeo.binary.metadata.internals.operations
        .TriggerMetadataMappingOnDocument;
import org.nuxeo.binary.metadata.internals.operations
        .WriteMetadataToBinaryFromContext;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 7.1
 */
@Ignore("NXBT-876")
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy({ "org.nuxeo.binary.metadata:binary-metadata-contrib-test.xml",
        "org.nuxeo.binary.metadata:binary-metadata-disable-listener.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD, init = BinaryMetadataServerInit.class)
public class TestBinaryMetadataOperation {

    @Inject
    AutomationService automationService;

    @Inject
    OperationContext operationContext;

    @Inject
    BinaryMetadataService binaryMetadataService;

    @Inject
    CoreSession session;

    private static final Map<String, Object> triggerParameters;

    static {
        triggerParameters = new HashMap<>();
        triggerParameters.put("metadataMappingId", "PDF");
    }

    private static final Properties jpgMetadata;

    private static final Map<String, Object> jpgParameters;

    static {
        jpgParameters = new HashMap<>();
        jpgMetadata = new Properties();
        jpgMetadata.put("EXIF:Model", "Platform");
        jpgMetadata.put("EXIF:Make", "Nuxeo");
        jpgParameters.put("metadata", jpgMetadata);
    }

    @Test
    public void itShouldExtractBinaryMetadata() throws OperationException {
        // Get mp3 file
        DocumentModel musicFile = BinaryMetadataServerInit.getFile(0, session);
        BlobHolder musicBlobHolder = musicFile.getAdapter(BlobHolder.class);
        operationContext.setInput(musicBlobHolder.getBlob());
        @SuppressWarnings("unchecked")
        Map<String, Object> blobProperties = (Map<String, Object>) automationService.run(operationContext,
                ReadMetadataFromBinary.ID);
        assertNotNull(blobProperties);
        assertEquals(48, blobProperties.size());
        assertEquals("Twist", blobProperties.get("ID3:Title").toString());
        assertEquals("Divine Recordings", blobProperties.get("ID3:Publisher").toString());
    }

    @Test
    public void itShouldApplyMetadataMapping() throws OperationException {
        // Get PDF document.
        DocumentModel pdfDoc = BinaryMetadataServerInit.getFile(1, session);
        operationContext.setInput(pdfDoc);
        operationContext.setCoreSession(session);
        automationService.run(operationContext, TriggerMetadataMappingOnDocument.ID, triggerParameters);
        pdfDoc = BinaryMetadataServerInit.getFile(1, session);
        assertEquals("en-US", pdfDoc.getPropertyValue("dc:title"));
        assertEquals("OpenOffice.org 3.2", pdfDoc.getPropertyValue("dc:source"));
    }

    @Test
    public void itShouldWriteMetadataOnBinary() throws OperationException {
        // Get PSD Document
        DocumentModel jpgFile = BinaryMetadataServerInit.getFile(4, session);
        BlobHolder jpgBlobHolder = jpgFile.getAdapter(BlobHolder.class);

        // Check the content
        Map<String, Object> blobProperties = binaryMetadataService.readMetadata(jpgBlobHolder.getBlob());
        assertNotNull(blobProperties);
        assertEquals("Google", blobProperties.get("EXIF:Make"));
        assertEquals("Nexus", blobProperties.get("EXIF:Model").toString());

        operationContext.setInput(jpgBlobHolder.getBlob());
        operationContext.setCoreSession(session);
        automationService.run(operationContext, WriteMetadataToBinaryFromContext.ID, jpgParameters);

        // Check the content
        blobProperties = binaryMetadataService.readMetadata(jpgBlobHolder.getBlob());
        assertNotNull(blobProperties);
        assertEquals("Nuxeo", blobProperties.get("EXIF:Make"));
        assertEquals("Platform", blobProperties.get("EXIF:Model").toString());
    }

}
