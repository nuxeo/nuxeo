/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.quota.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.quota.size.QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.ecm.quota.QuotaStatsInitialWork;
import org.nuxeo.ecm.quota.QuotaStatsService;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocument;
import org.nuxeo.ecm.quota.size.QuotaExceededException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RandomBug;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Various test that verify quota Sizes are updated accordingly to various operations. Due to the asynchronous nature of
 * size update, these test rely on {@link Thread#sleep(long)} and can fail because of that.
 *
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ QuotaFeature.class })
@TransactionalConfig
@RandomBug.Repeat(issue = "NXP-14442, NXP-14444")
public class TestDocumentsSizeUpdater {

    @Inject
    protected QuotaStatsService quotaStatsService;

    @Inject
    protected CoreSession session;

    @Inject
    RepositorySettings settings;

    @Inject
    protected EventService eventService;

    @Inject
    protected UserWorkspaceService uwm;

    protected DocumentRef wsRef;

    protected DocumentRef firstFolderRef;

    protected DocumentRef secondFolderRef;

    protected DocumentRef firstSubFolderRef;

    protected DocumentRef secondSubFolderRef;

    protected DocumentRef firstFileRef;

    protected DocumentRef secondFileRef;

    private IsolatedSessionRunner isr;

    protected static final boolean verboseMode = false;

    @Before
    public void cleanupSessionAssociationBeforeTest() throws Exception {
        isr = new IsolatedSessionRunner(session, eventService);
    }

    @Test
    public void testQuotaOnAddContent() throws Exception {

        addContent();
        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                assertQuota(getFirstFile(), 100L, 100L);
                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(getFirstSubFolder(), 0L, 300L);
                assertQuota(getFirstFolder(), 0L, 300L);
                assertQuota(getWorkspace(), 0L, 300L);

            }
        });

    }

    @Test
    public void testQuotaOnAddAndModifyContent() throws Exception {

        addContent();
        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                assertQuota(getFirstFile(), 100L, 100L);
                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(getFirstSubFolder(), 0L, 300L);
                assertQuota(getFirstFolder(), 0L, 300L);
                assertQuota(getWorkspace(), 0L, 300L);

            }
        });

        doUpdateContent();

        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                dump();

                assertQuota(getFirstFile(), 380L, 380L);
                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(getFirstSubFolder(), 0L, 580L);
                assertQuota(getFirstFolder(), 0L, 580L);
                assertQuota(getWorkspace(), 50L, 630L);
            }
        });

    }

    @Test
    public void testQuotaInitialCheckIn() throws Exception {

        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                assertQuota(getFirstFile(), 100, 100, 0, 0);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
                assertQuota(getFirstFolder(), 0, 300, 0, 0);
                assertQuota(getWorkspace(), 0, 300, 0, 0);
            }
        });

        doCheckIn();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                DocumentModel firstFile = getFirstFile();
                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.1", firstFile.getVersionLabel());

                // checked in: We count immediatly because at user (UI) level
                // checkout is not a visible action
                assertQuota(firstFile, 100, 200, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
                assertQuota(getFirstFolder(), 0, 400, 0, 100);
                assertQuota(getWorkspace(), 0, 400, 0, 100);
            }
        });

        // checkout the doc
        doCheckOut();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                DocumentModel firstFile = getFirstFile();
                assertTrue(firstFile.isCheckedOut());
                assertEquals("0.1+", firstFile.getVersionLabel());

                assertQuota(firstFile, 100, 200, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
                assertQuota(getFirstFolder(), 0, 400, 0, 100);
                assertQuota(getWorkspace(), 0, 400, 0, 100);
            }
        });
    }

    @Test
    public void testQuotaOnCheckInCheckOut() throws Exception {

        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

            }
        });

        doCheckIn();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel firstFile = getFirstFile();
                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.1", firstFile.getVersionLabel());

                assertQuota(firstFile, 100, 200, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
                assertQuota(getFirstFolder(), 0, 400, 0, 100);
                assertQuota(getWorkspace(), 0, 400, 0, 100);
            }
        });

        // checkout the doc
        doCheckOut();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                DocumentModel firstFile = getFirstFile();
                assertTrue(firstFile.isCheckedOut());
                assertEquals("0.1+", firstFile.getVersionLabel());

                assertQuota(firstFile, 100, 200, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 400, 0, 100);
                assertQuota(getFirstFolder(), 0, 400, 0, 100);
                assertQuota(getWorkspace(), 0, 400, 0, 100);

            }
        });
    }

    @Test
    public void testQuotaOnVersions() throws Exception {

        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                assertQuota(getFirstFile(), 100, 100, 0, 0);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
                assertQuota(getFirstFolder(), 0, 300, 0, 0);
                assertQuota(getWorkspace(), 0, 300, 0, 0);

            }
        });

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                DocumentModel firstFile = getFirstFile();
                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.1", firstFile.getVersionLabel());

                assertQuota(firstFile, 380, 480, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 680, 0, 100);
                assertQuota(getFirstFolder(), 0, 680, 0, 100);
                assertQuota(getWorkspace(), 50, 730, 0, 100);

            }
        });

        // create another version
        doSimpleVersion();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                DocumentModel firstFile = getFirstFile();

                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.2", firstFile.getVersionLabel());

                assertQuota(firstFile, 380, 860, 0, 480);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 1060, 0, 480);
                assertQuota(getFirstFolder(), 0, 1060, 0, 480);
                assertQuota(getWorkspace(), 50, 1110, 0, 480);

            }
        });

        // remove a version
        doRemoveFirstVersion();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                DocumentModel firstFile = getFirstFile();

                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.2", firstFile.getVersionLabel());

                assertQuota(firstFile, 380, 760, 0, 380);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 960, 0, 380);
                assertQuota(getFirstFolder(), 0, 960, 0, 380);
                assertQuota(getWorkspace(), 50, 1010, 0, 380);

            }
        });
        // remove doc and associated version

        doRemoveContent();
        eventService.waitForAsyncCompletion();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertFalse(session.exists(firstFileRef));

                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 200, 0, 0);
                assertQuota(getFirstFolder(), 0, 200, 0, 0);
                assertQuota(getWorkspace(), 50, 250, 0, 0);

            }
        });

    }

    @Test
    public void testQuotaOnMoveContent() throws Exception {
        // Given some content
        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();
            }
        });

        // When i Move the content
        doMoveContent();

        // Then the quota ore computed accordingly
        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertQuota(getFirstFile(), 100L, 100L);
                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(getFirstSubFolder(), 0L, 200L);
                assertQuota(getSecondSubFolder(), 0L, 100L);
                assertQuota(getFirstFolder(), 0L, 300L);
                assertQuota(getWorkspace(), 0L, 300L);

            }
        });

    }

    @Test
    public void testQuotaOnRemoveContent() throws Exception {

        addContent();
        doRemoveContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertFalse(session.exists(firstFileRef));

                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(getFirstSubFolder(), 0L, 200L);
                assertQuota(getFirstFolder(), 0L, 200L);
                assertQuota(getWorkspace(), 0L, 200L);

            }
        });

    }

    @Test
    public void testQuotaOnCopyContent() throws Exception {

        addContent();

        dump();

        doCopyContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                DocumentModel copiedFile = session.getChildren(secondSubFolderRef).get(0);

                assertQuota(getFirstFile(), 100L, 100L);
                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(copiedFile, 100L, 100L);
                assertQuota(getFirstSubFolder(), 0L, 300L);
                assertQuota(getSecondSubFolder(), 0L, 100L);
                assertQuota(getFirstFolder(), 0L, 400L);
                assertQuota(getWorkspace(), 0L, 400L);

            }
        });

    }

    @Test
    public void testQuotaOnCopyFolderishContent() throws Exception {

        addContent();

        dump();

        doCopyFolderishContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();
                DocumentModel copiedFolder = session.getChildren(secondFolderRef).get(0);

                DocumentModel copiedFirstFile = session.getChild(copiedFolder.getRef(), "file1");
                DocumentModel copiedSecondFile = session.getChild(copiedFolder.getRef(), "file2");

                assertQuota(getFirstFile(), 100L, 100L);
                assertQuota(getSecondFile(), 200L, 200L);
                assertQuota(getFirstSubFolder(), 0L, 300L);
                assertQuota(getFirstFolder(), 0L, 300L);
                assertQuota(getSecondFolder(), 0L, 300L);
                assertQuota(copiedFolder, 0L, 300L);
                assertQuota(copiedFirstFile, 100L, 100L);
                assertQuota(copiedSecondFile, 200L, 200L);
                assertQuota(getWorkspace(), 0L, 600L);

            }
        });

    }

    @Test
    public void testQuotaOnRemoveFoldishContent() throws Exception {
        addContent();

        dump();

        doRemoveFolderishContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();
                assertFalse(session.exists(firstSubFolderRef));

                assertQuota(getFirstFolder(), 0L, 0L);
                assertQuota(getWorkspace(), 0L, 0L);

            }
        });

    }

    @Test
    public void testQuotaOnMoveFoldishContent() throws Exception {

        addContent();

        dump();

        doMoveFolderishContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertEquals(1, session.getChildren(firstFolderRef).size());

                assertQuota(getWorkspace(), 0L, 300L);
                assertQuota(getFirstFolder(), 0L, 0L);
                assertQuota(getSecondFolder(), 0L, 300L);
                assertQuota(getFirstSubFolder(), 0L, 300L);
                assertQuota(getFirstFile(), 100L, 100L);
                assertQuota(getSecondFile(), 200L, 200L);

            }
        });

    }

    @Test
    public void testQuotaExceeded() throws Exception {

        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                // now add quota limit
                QuotaAware qa = getWorkspace().getAdapter(QuotaAware.class);
                assertNotNull(qa);

                assertEquals(300L, qa.getTotalSize());
                assertEquals(-1L, qa.getMaxQuota());

                // set the quota to 400
                qa.setMaxQuota(400L, true);
            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                boolean canNotExceedQuota = false;
                try {
                    // now try to update one
                    DocumentModel firstFile = session.getDocument(firstFileRef);
                    firstFile.setPropertyValue("file:content", (Serializable) getFakeBlob(250));
                    firstFile = session.saveDocument(firstFile);
                } catch (Exception e) {
                    if (QuotaExceededException.isQuotaExceededException(e)) {
                        System.out.println("raised expected Exception " + QuotaExceededException.unwrap(e).getMessage());
                        canNotExceedQuota = true;
                    }
                    TransactionHelper.setTransactionRollbackOnly();
                }
                assertTrue(canNotExceedQuota);
            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                // now remove the quota limit
                QuotaAware qa = getWorkspace().getAdapter(QuotaAware.class);
                assertNotNull(qa);

                assertEquals(300L, qa.getTotalSize());
                assertEquals(400L, qa.getMaxQuota());

                // set the quota to -1 / unlimited
                qa.setMaxQuota(-1L, true);

            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                Boolean canNotExceedQuota = false;
                try {
                    // now try to update one
                    DocumentModel firstFile = session.getDocument(firstFileRef);
                    firstFile.setPropertyValue("file:content", (Serializable) getFakeBlob(250));
                    firstFile = session.saveDocument(firstFile);
                } catch (Exception e) {
                    if (QuotaExceededException.isQuotaExceededException(e)) {
                        System.out.println("raised expected Exception " + QuotaExceededException.unwrap(e).getMessage());
                        canNotExceedQuota = true;
                    }
                    TransactionHelper.setTransactionRollbackOnly();
                }
                assertFalse(canNotExceedQuota);
            }
        });

    }

    @Test
    public void testQuotaExceededOnVersion() throws Exception {

        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                // now add quota limit
                DocumentModel ws = session.getDocument(wsRef);
                QuotaAware qa = ws.getAdapter(QuotaAware.class);
                assertNotNull(qa);

                assertEquals(300L, qa.getTotalSize());

                assertEquals(-1L, qa.getMaxQuota());

                // set the quota to 350
                qa.setMaxQuota(350L, true);

            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                dump();

                // create a version
                DocumentModel firstFile = session.getDocument(firstFileRef);
                firstFile.checkIn(VersioningOption.MINOR, null);

                boolean canNotExceedQuota = false;
                try {
                    // now try to checkout
                    firstFile.checkOut();
                } catch (Exception e) {
                    if (QuotaExceededException.isQuotaExceededException(e)) {
                        System.out.println("raised expected Exception " + QuotaExceededException.unwrap(e).getMessage());
                        canNotExceedQuota = true;
                    }
                    TransactionHelper.setTransactionRollbackOnly();
                }

                dump();

                assertTrue(canNotExceedQuota);
            }
        });

    }

    @Test
    public void testComputeInitialStatistics() throws Exception {

        EventServiceAdmin eventAdmin = Framework.getLocalService(EventServiceAdmin.class);
        eventAdmin.setListenerEnabledFlag("quotaStatsListener", false);

        addContent();
        doCheckIn();
        doDeleteFileContent(secondFileRef);

        dump();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel ws = session.getDocument(wsRef);
                assertFalse(ws.hasFacet(QuotaAwareDocument.DOCUMENTS_SIZE_STATISTICS_FACET));

                String updaterName = "documentsSizeUpdater";
                quotaStatsService.launchInitialStatisticsComputation(updaterName, session.getRepositoryName());
                WorkManager workManager = Framework.getLocalService(WorkManager.class);
                String queueId = workManager.getCategoryQueueId(QuotaStatsInitialWork.CATEGORY_QUOTA_INITIAL);

                workManager.awaitCompletion(queueId, 10, TimeUnit.SECONDS);

            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
                assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
                assertQuota(getFirstSubFolder(), 0L, 400L, 200L, 100L);
                assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
                assertQuota(getWorkspace(), 0L, 400L, 200L, 100L);

            }
        });

        eventAdmin.setListenerEnabledFlag("quotaStatsListener", true);

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel firstFile = getFirstFile();
                // modify file to checkout
                firstFile.setPropertyValue("dc:title", "File1");
                firstFile = session.saveDocument(firstFile);
                session.save(); // process invalidations
                assertTrue(firstFile.isCheckedOut());
            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                assertQuota(getFirstFile(), 100L, 200L, 0L, 100L);
                assertQuota(getSecondFile(), 200L, 200L, 200L, 0L);
                assertQuota(getFirstSubFolder(), 0L, 400L, 200L, 100L);
                assertQuota(getFirstFolder(), 0L, 400L, 200L, 100L);
                assertQuota(getWorkspace(), 0L, 400L, 200L, 100L);

            }
        });
    }

    @Test
    public void testQuotaOnDeleteContent() throws Exception {
        addContent();
        doDeleteFileContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel ws = session.getDocument(wsRef);
                DocumentModel firstFolder = session.getDocument(firstFolderRef);
                DocumentModel firstFile = session.getDocument(firstFileRef);

                assertQuota(firstFile, 100L, 100L, 100L);
                assertQuota(firstFolder, 0L, 300L, 100L);
                assertQuota(ws, 0L, 300L, 100L);

            }
        });

        doUndeleteFileContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                assertQuota(getFirstFile(), 100L, 100L, 0L);
                assertQuota(getFirstFolder(), 0L, 300L, 0L);
                assertQuota(getWorkspace(), 0L, 300L, 0L);

            }
        });

        // sent file to trash
        doDeleteFileContent();
        // then permanently delete file when file is in trash
        doRemoveContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                assertFalse(session.exists(firstFileRef));
                assertQuota(getFirstFolder(), 0L, 200L, 0L);
                assertQuota(getWorkspace(), 0L, 200L, 0L);

            }
        });
    }

    @Test
    public void testQuotaOnMoveContentWithVersions() throws Exception {

        addContent();
        // update and create a version
        doUpdateAndVersionContent();
        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                assertQuota(getFirstFile(), 380, 480, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 680, 0, 100);
                assertQuota(getFirstFolder(), 0, 680, 0, 100);
                assertQuota(getWorkspace(), 50, 730, 0, 100);

            }
        });

        doMoveFileContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                assertQuota(getFirstFile(), 380, 480, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 200, 0, 0);
                assertQuota(getFirstFolder(), 0, 200, 0, 0);
                assertQuota(getSecondFolder(), 0, 480, 0, 100);
                assertQuota(getWorkspace(), 50, 730, 0, 100);

            }
        });

    }

    @Test
    public void testQuotaOnCopyContentWithVersions() throws Exception {

        addContent();
        // update and create a version
        doUpdateAndVersionContent();
        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertQuota(getFirstFile(), 380, 480, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 680, 0, 100);
                assertQuota(getFirstFolder(), 0, 680, 0, 100);
                assertQuota(getWorkspace(), 50, 730, 0, 100);

            }
        });

        doCopyContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();
                DocumentModel copiedFile = session.getChildren(secondSubFolderRef).get(0);

                assertQuota(getFirstFile(), 380, 480, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 680, 0, 100);
                assertQuota(getFirstFolder(), 0, 1060, 0, 100);
                assertQuota(copiedFile, 380, 380, 0, 0);
                assertQuota(getSecondSubFolder(), 0, 380, 0, 0);
                assertQuota(getWorkspace(), 50, 1110, 0, 100);

            }
        });
    }

    @Test
    public void testAllowSettingMaxQuota() throws Exception {
        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                // now add quota limit
                DocumentModel ws = session.getDocument(wsRef);
                QuotaAware qa = ws.getAdapter(QuotaAware.class);
                assertNotNull(qa);
                // set the quota to 400
                qa.setMaxQuota(400L, true);

                DocumentModel firstSubFolder = session.getDocument(firstSubFolderRef);
                QuotaAware qaFSF = firstSubFolder.getAdapter(QuotaAware.class);
                qaFSF.setMaxQuota(200L, true);

            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel secondFolder = session.getDocument(secondFolderRef);
                QuotaAware qaSecFolder = secondFolder.getAdapter(QuotaAware.class);
                boolean canSetQuota = true;
                try {
                    qaSecFolder.setMaxQuota(300L, true);
                } catch (QuotaExceededException e) {
                    canSetQuota = false;
                }
                assertFalse(canSetQuota);
                qaSecFolder.setMaxQuota(200L, true);
            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                QuotaAware qaSecFolder = getSecondFolder().getAdapter(QuotaAware.class);
                assertEquals(200L, qaSecFolder.getMaxQuota());

                QuotaAware qaFirstFolder = getFirstFolder().getAdapter(QuotaAware.class);

                boolean canSetQuota = true;
                try {
                    qaFirstFolder.setMaxQuota(50L, true);
                } catch (QuotaExceededException e) {
                    canSetQuota = false;
                }
                assertFalse(canSetQuota);

                QuotaAware qaSecSubFolder = getSecondSubFolder().getAdapter(QuotaAware.class);
                canSetQuota = true;
                try {
                    qaSecSubFolder.setMaxQuota(50L, true);
                } catch (QuotaExceededException e) {
                    canSetQuota = false;
                }
                assertFalse(canSetQuota);
            }
        });
    }

    @Test
    public void testQuotaOnRevertVersion() throws Exception {

        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                assertQuota(getFirstFile(), 100, 100, 0, 0);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 300, 0, 0);
                assertQuota(getFirstFolder(), 0, 300, 0, 0);
                assertQuota(getWorkspace(), 0, 300, 0, 0);

            }
        });

        // update and create a version
        doUpdateAndVersionContent();
        // ws + 50, file + 280 + version

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                DocumentModel firstFile = getFirstFile();
                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.1", firstFile.getVersionLabel());

                assertQuota(firstFile, 380, 480, 0, 100);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 680, 0, 100);
                assertQuota(getFirstFolder(), 0, 680, 0, 100);
                assertQuota(getWorkspace(), 50, 730, 0, 100);

            }
        });
        // create another version
        doSimpleVersion();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel firstFile = getFirstFile();
                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.2", firstFile.getVersionLabel());

                assertQuota(firstFile, 380, 860, 0, 480);
                assertQuota(getSecondFile(), 200, 200, 0, 0);
                assertQuota(getFirstSubFolder(), 0, 1060, 0, 480);
                assertQuota(getFirstFolder(), 0, 1060, 0, 480);
                assertQuota(getWorkspace(), 50, 1110, 0, 480);

                List<DocumentModel> versions = session.getVersions(firstFileRef);
                for (DocumentModel documentModel : versions) {
                    if ("0.1".equals(documentModel.getVersionLabel())) {
                        firstFile = session.restoreToVersion(firstFileRef, documentModel.getRef(), true, true);
                    }
                }
                firstFileRef = firstFile.getRef();
            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                dump();

                DocumentModel firstFile = getFirstFile();
                assertFalse(firstFile.isCheckedOut());
                assertEquals("0.1", firstFile.getVersionLabel());

                assertQuota(firstFile, 380, 760, 0, 760);
                assertQuota(getFirstSubFolder(), 0, 960, 0, 760);
                assertQuota(getFirstFolder(), 0, 960, 0, 760);
                assertQuota(getWorkspace(), 50, 1010, 0, 760);

            }
        });
    }

    @Test
    public void testAllowSettingMaxQuotaOnUserWorkspace() throws Exception {
        addContent();

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                try (CoreSession userSession = settings.openSessionAs("toto")) {
                    DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
                    assertNotNull(uw);
                }
                try (CoreSession userSession = settings.openSessionAs("titi")) {
                    DocumentModel uw = uwm.getCurrentUserPersonalWorkspace(userSession, null);
                    assertNotNull(uw);
                }
                quotaStatsService.activateQuotaOnUserWorkspaces(300L, session);
                quotaStatsService.launchSetMaxQuotaOnUserWorkspaces(300L, session.getRootDocument(), session);

            }
        });

        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        workManager.awaitCompletion("quota", 3, TimeUnit.SECONDS);
        assertEquals(0, workManager.getQueueSize("quota", null));

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {
                DocumentModel totoUW = uwm.getUserPersonalWorkspace("toto",
                        session.getDocument(new PathRef("/default-domain/")));
                QuotaAware totoUWQuota = totoUW.getAdapter(QuotaAware.class);
                assertEquals(300L, totoUWQuota.getMaxQuota());
                DocumentModel titiUW = uwm.getUserPersonalWorkspace("titi",
                        session.getDocument(new PathRef("/default-domain/")));
                QuotaAware titiUWQuota = titiUW.getAdapter(QuotaAware.class);
                assertEquals(300L, titiUWQuota.getMaxQuota());

                // create content in the 2 user workspaces
                DocumentModel firstFile = session.createDocumentModel(totoUW.getPathAsString(), "file1", "File");
                firstFile.setPropertyValue("file:content", (Serializable) getFakeBlob(200));
                firstFile = session.createDocument(firstFile);
                firstFile = session.saveDocument(firstFile);

                DocumentModel secondFile = session.createDocumentModel(titiUW.getPathAsString(), "file2", "File");
                secondFile.setPropertyValue("file:content", (Serializable) getFakeBlob(200));
                secondFile = session.createDocument(secondFile);
                secondFile = session.saveDocument(secondFile);

            }
        });

        isr.run(new RunnableWithException() {

            @Override
            public void run() throws Exception {

                DocumentModel totoUW = uwm.getUserPersonalWorkspace("toto",
                        session.getDocument(new PathRef("/default-domain/")));
                QuotaAware totoUWQuota = totoUW.getAdapter(QuotaAware.class);
                assertEquals(200L, totoUWQuota.getTotalSize());
                DocumentModel titiUW = uwm.getUserPersonalWorkspace("titi",
                        session.getDocument(new PathRef("/default-domain/")));
                QuotaAware titiUWQuota = titiUW.getAdapter(QuotaAware.class);
                assertEquals(200L, titiUWQuota.getTotalSize());

                boolean canAddContent = true;
                try {
                    DocumentModel secondFile = session.createDocumentModel(titiUW.getPathAsString(), "file2", "File");
                    secondFile.setPropertyValue("file:content", (Serializable) getFakeBlob(200));
                    secondFile = session.createDocument(secondFile);
                    secondFile = session.saveDocument(secondFile);
                } catch (Exception e) {
                    if (e.getCause() instanceof QuotaExceededException) {
                        canAddContent = false;
                    }
                }
                assertFalse(canAddContent);

                canAddContent = true;
                try {
                    DocumentModel firstFile = session.createDocumentModel(totoUW.getPathAsString(), "file1", "File");
                    firstFile.setPropertyValue("file:content", (Serializable) getFakeBlob(200));
                    firstFile = session.createDocument(firstFile);
                    firstFile = session.saveDocument(firstFile);
                } catch (Exception e) {
                    if (e.getCause() instanceof QuotaExceededException) {
                        canAddContent = false;
                    }
                }
                assertFalse(canAddContent);
            }
        });
    }

    protected Blob getFakeBlob(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append('a');
        }
        Blob blob = new StringBlob(sb.toString());
        blob.setMimeType("text/plain");
        blob.setFilename("FakeBlob_" + size + ".txt");
        return blob;
    }

    protected void addContent() throws Exception {
        addContent(false);
    }

    protected void addContent(final boolean checkInFirstFile) throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
                ws = session.createDocument(ws);
                ws = session.saveDocument(ws);
                wsRef = ws.getRef();

                DocumentModel firstFolder = session.createDocumentModel(ws.getPathAsString(), "folder1", "Folder");
                firstFolder = session.createDocument(firstFolder);
                firstFolder = session.saveDocument(firstFolder);
                firstFolderRef = firstFolder.getRef();

                DocumentModel firstSubFolder = session.createDocumentModel(firstFolder.getPathAsString(), "subfolder1",
                        "Folder");
                firstSubFolder = session.createDocument(firstSubFolder);
                firstSubFolder = session.saveDocument(firstSubFolder);

                firstSubFolderRef = firstSubFolder.getRef();

                DocumentModel firstFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file1", "File");
                firstFile.setPropertyValue("file:content", (Serializable) getFakeBlob(100));
                firstFile = session.createDocument(firstFile);
                firstFile = session.saveDocument(firstFile);
                if (checkInFirstFile) {
                    firstFile.checkIn(VersioningOption.MINOR, null);
                }

                firstFileRef = firstFile.getRef();

                DocumentModel secondFile = session.createDocumentModel(firstSubFolder.getPathAsString(), "file2",
                        "File");
                secondFile.setPropertyValue("file:content", (Serializable) getFakeBlob(200));

                secondFile = session.createDocument(secondFile);
                secondFile = session.saveDocument(secondFile);
                secondFileRef = secondFile.getRef();

                DocumentModel secondSubFolder = session.createDocumentModel(firstFolder.getPathAsString(),
                        "subfolder2", "Folder");
                secondSubFolder = session.createDocument(secondSubFolder);
                secondSubFolder = session.saveDocument(secondSubFolder);
                secondSubFolderRef = secondSubFolder.getRef();

                DocumentModel secondFolder = session.createDocumentModel(ws.getPathAsString(), "folder2", "Folder");
                secondFolder = session.createDocument(secondFolder);
                secondFolder = session.saveDocument(secondFolder);
                secondFolderRef = secondFolder.getRef();
            }
        });
    }

    protected void doMoveContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                session.move(firstFileRef, secondSubFolderRef, null);
            }
        });
    }

    protected void doMoveFolderishContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                session.move(firstSubFolderRef, secondFolderRef, null);
            }
        });
    }

    protected void doMoveFileContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                session.move(firstFileRef, secondFolderRef, null);
            }
        });
    }

    protected void doUpdateContent() throws Exception {
        if (verboseMode) {
            System.out.println("Update content");
        }
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                DocumentModel ws = session.getDocument(wsRef);
                DocumentModel firstFile = session.getDocument(firstFileRef);

                ws.setPropertyValue("file:content", (Serializable) getFakeBlob(50));
                ws = session.saveDocument(ws);

                List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();

                for (int i = 1; i < 5; i++) {
                    Map<String, Serializable> files_entry = new HashMap<String, Serializable>();
                    files_entry.put("filename", "fakefile" + i);
                    files_entry.put("file", (Serializable) getFakeBlob(70));
                    files.add(files_entry);
                }

                firstFile.setPropertyValue("files:files", (Serializable) files);
                firstFile = session.saveDocument(firstFile);
            }
        });

    }

    protected void doCheckIn() throws Exception {
        if (verboseMode) {
            System.out.println("CheckIn first file");
        }
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                DocumentModel firstFile = session.getDocument(firstFileRef);
                firstFile.checkIn(VersioningOption.MINOR, null);
            }
        });
    }

    protected void doCheckOut() throws Exception {
        if (verboseMode) {
            System.out.println("CheckOut first file");
        }
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                DocumentModel firstFile = session.getDocument(firstFileRef);
                firstFile.checkOut();
            }
        });
    }

    protected void doUpdateAndVersionContent() throws Exception {
        if (verboseMode) {
            System.out.println("Update content and create version ");
        }
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                DocumentModel ws = session.getDocument(wsRef);
                DocumentModel firstFile = session.getDocument(firstFileRef);

                ws.setPropertyValue("file:content", (Serializable) getFakeBlob(50));
                ws = session.saveDocument(ws);

                List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();

                for (int i = 1; i < 5; i++) {
                    Map<String, Serializable> files_entry = new HashMap<String, Serializable>();
                    files_entry.put("filename", "fakefile" + i);
                    files_entry.put("file", (Serializable) getFakeBlob(70));
                    files.add(files_entry);
                }

                firstFile.setPropertyValue("files:files", (Serializable) files);
                // create minor version
                firstFile.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
                firstFile = session.saveDocument(firstFile);
            }
        });
    }

    protected void doSimpleVersion() throws Exception {
        if (verboseMode) {
            System.out.println("simply create a version ");
        }
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                DocumentModel firstFile = session.getDocument(firstFileRef);

                firstFile.setPropertyValue("dc:title", "a version");
                firstFile.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
                firstFile = session.saveDocument(firstFile);
            }
        });
    }

    protected void doRemoveFirstVersion() throws Exception {
        if (verboseMode) {
            System.out.println("remove first created version ");
        }
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                List<DocumentModel> versions = session.getVersions(firstFileRef);

                session.removeDocument(versions.get(0).getRef());
            }
        });
    }

    protected void doRemoveContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                session.removeDocument(firstFileRef);
            }
        });
    }

    protected void doRemoveFolderishContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                session.removeDocument(firstSubFolderRef);
            }
        });
    }

    protected void doDeleteFileContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                List<DocumentModel> docs = new ArrayList<DocumentModel>();
                docs.add(session.getDocument(firstFileRef));
                Framework.getLocalService(TrashService.class).trashDocuments(docs);
            }
        });
    }

    protected void doDeleteFileContent(final DocumentRef fileRef) throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                List<DocumentModel> docs = new ArrayList<DocumentModel>();
                docs.add(session.getDocument(fileRef));
                Framework.getLocalService(TrashService.class).trashDocuments(docs);
            }
        });
    }

    protected void doUndeleteFileContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                List<DocumentModel> docs = new ArrayList<DocumentModel>();
                docs.add(session.getDocument(firstFileRef));
                Framework.getLocalService(TrashService.class).undeleteDocuments(docs);
            }
        });
    }

    protected void doCopyContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {
                session.copy(firstFileRef, secondSubFolderRef, null);
            }
        });
    }

    protected void doCopyFolderishContent() throws Exception {
        isr.run(new RunnableWithException() {
            @Override
            public void run() throws Exception {

                session.copy(firstSubFolderRef, secondFolderRef, null);
            }
        });
    }

    protected void dump() throws Exception {

        if (!verboseMode) {
            return;
        }
        System.out.println("\n####################################\n");
        DocumentModelList docs = session.query("select * from Document order by ecm:path");
        for (DocumentModel doc : docs) {
            if (doc.isVersion()) {
                System.out.print(" --version ");
            }
            System.out.print(doc.getId() + " " + doc.getPathAsString());
            if (doc.hasSchema("uid")) {
                System.out.print(" (" + doc.getPropertyValue("uid:major_version") + "."
                        + doc.getPropertyValue("uid:minor_version") + ")");
            }

            if (doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET)) {
                QuotaAware qa = doc.getAdapter(QuotaAware.class);
                System.out.println(" [ quota : " + qa.getTotalSize() + "(" + qa.getInnerSize() + ") / "
                        + qa.getMaxQuota() + "]");
                // System.out.println(" with Quota facet");
            } else {
                System.out.println(" no Quota facet !!!");
            }
        }

    }

    protected void assertQuota(DocumentModel doc, long innerSize, long totalSize) {
        assertTrue(doc.hasFacet(DOCUMENTS_SIZE_STATISTICS_FACET));
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals("inner:" + innerSize + " total:" + totalSize,
                "inner:" + qa.getInnerSize() + " total:" + qa.getTotalSize());
    }

    protected void assertQuota(DocumentModel doc, long innerSize, long totalSize, long trashSize) {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals("inner:" + innerSize + " total:" + totalSize + " trash:" + trashSize, "inner:" + qa.getInnerSize()
                + " total:" + qa.getTotalSize() + " trash:" + qa.getTrashSize());
    }

    protected void assertQuota(DocumentModel doc, long innerSize, long totalSize, long trashSize, long versionsSize) {
        QuotaAware qa = doc.getAdapter(QuotaAware.class);
        assertNotNull(qa);
        assertEquals("inner:" + innerSize + " total:" + totalSize + " trash:" + trashSize + " versions: "
                + versionsSize,
                "inner:" + qa.getInnerSize() + " total:" + qa.getTotalSize() + " trash:" + qa.getTrashSize()
                        + " versions: " + qa.getVersionsSize());
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getWorkspace() throws ClientException {
        return session.getDocument(wsRef);
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getFirstSubFolder() throws ClientException {
        return session.getDocument(firstSubFolderRef);
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getSecondSubFolder() throws ClientException {
        return session.getDocument(secondSubFolderRef);
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getFirstFolder() throws ClientException {
        return session.getDocument(firstFolderRef);
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getSecondFolder() throws ClientException {
        return session.getDocument(secondFolderRef);
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getSecondFile() throws ClientException {
        return session.getDocument(secondFileRef);
    }

    /**
     * @return
     * @throws ClientException
     */
    protected DocumentModel getFirstFile() throws ClientException {
        return session.getDocument(firstFileRef);
    }

}
