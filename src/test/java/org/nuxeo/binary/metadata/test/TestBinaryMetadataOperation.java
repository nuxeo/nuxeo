/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.binary.metadata.api.operation.ReadMetadataFromBinary;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
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
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy({ "org.nuxeo.binary.metadata.test:OSGI-INF/binary-metadata-contrib-test.xml",
        "org.nuxeo.binary.metadata.test:OSGI-INF/binary-metadata-file-contrib-test.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD, init = BinaryMetadataServerInit.class)
public class TestBinaryMetadataOperation {

    @Inject
    AutomationService automationService;

    @Inject
    OperationContext operationContext;

    @Inject
    CoreSession session;

    @Test
    public void itShouldExtractBinaryMetadata() throws OperationException {
        // Get mp3 file
        DocumentModel musicFile = BinaryMetadataServerInit.getFile(0, session);
        BlobHolder musicBlobHolder = musicFile.getAdapter(BlobHolder.class);
        operationContext.setInput(musicBlobHolder.getBlob());
        Map<String, Object> blobProperties = (Map<String, Object>) automationService.run(operationContext,
                ReadMetadataFromBinary.ID);
        assertNotNull(blobProperties);
        assertEquals(49, blobProperties.size());
        assertEquals("Twist", blobProperties.get("ID3:Title").toString());
        assertEquals("Divine Recordings", blobProperties.get("ID3:Publisher").toString());
    }

}
