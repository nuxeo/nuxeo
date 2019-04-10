/*
 * (C) Copyright 2010-2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 *     Nuxeo - initial API and implementation
 *     <a href="mailto:hbrown@nuxeo.com">Harlan</a>
 */

package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.content.template")
@Deploy("org.nuxeo.ecm.platform.importer.core")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.video.core")
@Deploy("org.nuxeo.ecm.platform.audio.core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.importer.core.test:test-importer-with-filemanager-contrib.xml")
public class TestImporterWithFileManager {

    @Inject
    TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected DefaultImporterService importerService;

    @Test
    public void testImporterContribution() throws Exception {

        DocumentModel doc = session.createDocumentModel("/default-domain/workspaces", "ws1", "Workspace");
        doc = session.createDocument(doc);
        assertNotNull(doc);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        File source = FileUtils.getResourceFileFromContext("filemanager");
        importerService.importDocuments("/default-domain/workspaces/ws1", source.getPath(), false, 5, 5);
        session.save();
        txFeature.nextTransaction();

        DocumentModel file = session.getDocument(new PathRef("/default-domain/workspaces/ws1/filemanager/cat.gif"));
        assertNotNull(file);
        assertEquals("Picture", file.getType());

        file = session.getDocument(new PathRef("/default-domain/workspaces/ws1/filemanager/sample.wav"));
        assertNotNull(file);
        assertEquals("Audio", file.getType());

        file = session.getDocument(new PathRef("/default-domain/workspaces/ws1/filemanager/sample.mpg"));
        assertNotNull(file);
        assertEquals("Video", file.getType());

    }
}
