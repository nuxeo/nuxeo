/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.threed.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THREED_TYPE;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.filemanager.core", "org.nuxeo.ecm.platform.filemanager.api",
        "org.nuxeo.ecm.platform.types.core", "org.nuxeo.ecm.platform.types.api" })
@LocalDeploy({ "org.nuxeo.ecm.platform.threed.core:OSGI-INF/filemanager-contrib.xml",
        "org.nuxeo.ecm.platform.threed.core:OSGI-INF/core-types-contrib.xml" })
public class Test3DCore {

    @Inject
    private CoreSession session;

    @Inject
    private FileManager fileManager;

    @Test
    public void test() throws IOException {
        String path = "duck.dae";
        URL url = this.getClass().getClassLoader().getResource(path);
        File file = null;
        try {
            assert url != null;
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        assertNotNull(file);
        Blob blob = new FileBlob(file);
        DocumentModel doc = fileManager.createDocumentFromBlob(session, blob, "/", true, path);
        assertNotNull(doc);
        assertEquals(doc.getType(), THREED_TYPE);
        assertEquals(doc.getName(), blob.getFilename());
        assertEquals(doc.getPropertyValue("file:filename"), blob.getFilename());
        assertEquals(doc.getPropertyValue("file:content"), blob);
    }

}
