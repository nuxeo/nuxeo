/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.fileimporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.dam.Constants;
import org.nuxeo.dam.DamService;
import org.nuxeo.dam.platform.context.ImportActions;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DetectThreadDeadlocksFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.richfaces.event.UploadEvent;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DetectThreadDeadlocksFeature.class })
@RepositoryConfig(type = BackendType.H2, user = "Administrator")
@Deploy( { "org.nuxeo.ecm.platform.types.api",
        "org.nuxeo.ecm.platform.types.core",
        "org.nuxeo.ecm.platform.mimetype.core",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.platform.video.api",
        "org.nuxeo.ecm.platform.video.core",
        "org.nuxeo.ecm.platform.video.convert",
        "org.nuxeo.ecm.platform.audio.core",
        "org.nuxeo.ecm.platform.filemanager.core",
        "org.nuxeo.ecm.platform.importer.core", "org.nuxeo.dam.importer.core",
        "org.nuxeo.ecm.platform.content.template", "org.nuxeo.dam.core",
        "org.nuxeo.ecm.webapp.base", "org.nuxeo.ecm.webapp.core",
        "org.nuxeo.ecm.platform.picture.jsf" })
@LocalDeploy( { "org.nuxeo.dam.core:OSGI-INF/test-dam-content-template.xml" })
public class TestZipImporter {

    protected static final String IMPORT_FOLDER_NAME = "import-folder";

    protected static final String TEST_FILE_PATH = "test-data"+File.separator+"test.zip";

    protected static final String IMPORT_SET_QUERY = "SELECT * FROM Document WHERE ecm:primaryType = 'ImportSet'";

    protected final Log log = LogFactory.getLog(TestZipImporter.class);

    @Inject
    protected CoreSession session;

    @Before
    public void deactivateBinaryTextListener() {
        EventServiceAdmin eventServiceAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventServiceAdmin.setListenerEnabledFlag("sql-storage-binary-text",
                false);
    }

    @After
    public void checkShutdown() {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        ThreadMXBean threadMgmt = ManagementFactory.getThreadMXBean();
        boolean stillRunning = false;
        for (ThreadInfo info:threadMgmt.dumpAllThreads(false, false)) {
            String threadName = info.getThreadName();
            if (isRunningAsyncThread(info)) {
                Throwable context = new Throwable("Thread stack trace");
                context.setStackTrace(info.getStackTrace());
                log.warn(threadName + " is still running", context);
                stillRunning = true;
            }
        }
        if (stillRunning) {
            throw new Error("event services async thread are still running");
        }
    }

    protected boolean isRunningAsyncThread(ThreadInfo info) {
        if (!info.getThreadName().startsWith("Nuxeo Async")) {
            return false;
        }
        for (StackTraceElement element:info.getStackTrace()) {
            if (element.getClass().getCanonicalName().startsWith("org.nuxeo")) {
                return true;
            }
        }
        return false;
    }
    protected File getTestFile(String relativePath) {
        return new File(FileUtils.getResourcePathFromContext(relativePath));
    }

    @Test
    public void testImportSetCreation() throws Exception {

        ImportActions importActions = new ImportActionsMock(session);

        DocumentModel importSet = importActions.getNewImportSet();
        // test that we have a default title
        String defaultTitle = (String) importSet.getProperty("dublincore",
                "title");
        assertNotNull(defaultTitle);
        assertTrue(defaultTitle.startsWith("Administrator"));

        DamService damService = Framework.getLocalService(DamService.class);
        Path importFolderPath = new Path(damService.getAssetLibraryPath());
        importFolderPath = importFolderPath.append(IMPORT_FOLDER_NAME);
        DocumentModel importSetRoot = session.getDocument(new PathRef(
                importFolderPath.toString()));
        importActions.setImportFolder(importSetRoot.getId());

        File file = getTestFile(TEST_FILE_PATH);
        UploadEvent event = UploadItemMock.getUploadEvent(file);
        importActions.uploadListener(event);
        importSet.setProperty("dublincore", "title", "myimportset");

        importActions.createImportSet();

        session.save();
        DocumentModelList importSets = session.query(IMPORT_SET_QUERY);
        assertTrue(importSets.size() == 1);
        importSet = importSets.get(0);
        assertNotNull(importSet);

        String title = (String) importSet.getProperty("dublincore", "title");
        String type = importSet.getType();
        assertEquals("myimportset", title);
        assertEquals(Constants.IMPORT_SET_TYPE, type);

        DocumentModelList children = session.getChildren(importSet.getRef());
        assertNotNull(children);
        assertEquals(5, children.size());

        DocumentModel child = session.getChild(importSet.getRef(), "plain.txt");
        assertNotNull(child);
        assertEquals("Note", child.getType());

        /*
        child = session.getChild(importSet.getRef(), "image-jpg");
        assertNotNull(child);
        assertEquals("Picture", child.getType());
        */

        child = session.getChild(importSet.getRef(), "spreadsheet.xls");
        assertNotNull(child);
        assertEquals("File", child.getType());

        // names are converted to lowercase
        child = session.getChild(importSet.getRef(), "sampleWAV.wav");
        assertNotNull(child);
        assertEquals("Audio", child.getType());

        child = session.getChild(importSet.getRef(), "sampleMPG.mpg");
        assertNotNull(child);
        assertEquals("Video", child.getType());
    }

}
