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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.binary.metadata.api.service.BinaryMetadataService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 7.1
 */
@RunWith(FeaturesRunner.class)
@Features(BinaryMetadataFeature.class)
@LocalDeploy({ "org.nuxeo.binary.metadata.test:OSGI-INF/binary-metadata-contrib-test.xml",
        "org.nuxeo.binary.metadata.test:OSGI-INF/binary-metadata-file-contrib-test.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD, init = BinaryMetadataServerInit.class)
public class TestBinaryMetadataService {

    @Inject
    BinaryMetadataService binaryMetadataService;

    @Inject
    CoreSession session;

    List<String> musicMetadata = new ArrayList<String>() {
        {
            add("Title");
            add("Lyrics-por");
            add("Publisher");
            add("Comment");
        }
    };

    @Test
    public void itShouldExtractAllMetadataFromBinary() {
        DocumentModel musicFile = BinaryMetadataServerInit.getFile(0, session);
        BlobHolder musicBlobHolder = musicFile.getAdapter(BlobHolder.class);
        Map<String, Object> blobProperties = binaryMetadataService.readMetadata(musicBlobHolder.getBlob());
        assertNotNull(blobProperties);
        assertEquals("Twist", blobProperties.get("Title").toString());
        assertEquals("Divine Recordings", blobProperties.get("Publisher").toString());
    }

    @Test
    public void itShouldExtractGivenMetadataFromBinary() {
        DocumentModel musicFile = BinaryMetadataServerInit.getFile(0, session);
        BlobHolder musicBlobHolder = musicFile.getAdapter(BlobHolder.class);
        Map<String, Object> blobProperties = binaryMetadataService.readMetadata(musicBlobHolder.getBlob(), musicMetadata);
        assertNotNull(blobProperties);
        assertEquals("Twist", blobProperties.get("Title").toString());
        assertEquals("Divine Recordings", blobProperties.get("Publisher").toString());
    }
}
