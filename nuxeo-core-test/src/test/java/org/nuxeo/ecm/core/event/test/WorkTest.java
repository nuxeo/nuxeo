/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.test;

import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import junit.framework.Assert;

import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.storage.sql.IgnoreNonPooledCondition;
import org.nuxeo.ecm.core.storage.sql.TXSQLRepositoryTestCase;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.ConditionalIgnoreRule;
import org.nuxeo.runtime.transaction.TransactionHelper;

@ConditionalIgnoreRule.Ignore(condition=IgnoreNonPooledCondition.class)
public class WorkTest extends TXSQLRepositoryTestCase {

    @Rule
    public final ConditionalIgnoreRule ignoreRule = new ConditionalIgnoreRule();

    @Before
    public void checkAssumptions() {
        assumeTrue(hasPoolingConfig());
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.event");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (!hasPoolingConfig()) {
            return;
        }
        super.tearDown();
    }

    public void doTestWorkConcurrencyException(boolean explicitSave)
            throws Exception {
        DocumentModel folder = session.createDocumentModel("/", "folder",
                "Folder");
        folder = session.createDocument(folder);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        waitForAsyncCompletion();

        WorkManager workManager = Framework.getLocalService(WorkManager.class);

        // the addChildWork gets retried so must pass the latches the second
        // time, which is why we don't use a CyclicBarrier
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch proceed = new CountDownLatch(2);

        RemoveFolderWork removeFolderWork = new RemoveFolderWork();
        AddChildWork addChildWork = new AddChildWork();
        removeFolderWork.init(folder, ready, proceed, explicitSave);
        addChildWork.init(folder, ready, proceed, explicitSave);
        workManager.schedule(removeFolderWork);
        workManager.schedule(addChildWork);

        waitForAsyncCompletion();
        Assert.assertEquals(Arrays.asList(Boolean.TRUE, Boolean.FALSE),
                addChildWork.existList);
    }

    @Test
    public void testWorkConcurrencyExceptionExplicitSave() throws Exception {
        doTestWorkConcurrencyException(true);
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition=ConditionalIgnoreRule.NXP10926H2Upgrade.class)
    // no concurrent update detected in H2 1.4.177
    public void testWorkConcurrencyExceptionImplicitSave() throws Exception {
        doTestWorkConcurrencyException(false);
    }

    public static abstract class BaseWork extends AbstractWork {
        private static final long serialVersionUID = 1L;

        protected CountDownLatch ready;

        protected CountDownLatch proceed;

        protected boolean explicitSave;

        public void init(DocumentModel folder, CountDownLatch ready,
                CountDownLatch proceed, boolean explicitSave) {
            setDocument(folder.getRepositoryName(), folder.getId());
            this.ready = ready;
            this.proceed = proceed;
            this.explicitSave = explicitSave;
        }

        @Override
        public String getTitle() {
            return getClass().getName();
        }

        protected void ready() throws InterruptedException {
            ready.countDown();
            ready.await();
        }

        protected void proceed() throws InterruptedException {
            proceed.countDown();
            proceed.await();
        }
    }

    /*
     * The following 2 work instance are synced with a latch in order to add a
     * child after the folder is deleted.
     */

    /**
     * Removes the folder.
     */
    public static class RemoveFolderWork extends BaseWork {
        private static final long serialVersionUID = 1L;

        @Override
        public void work() throws Exception {
            initSession();
            ready();
            try {
                DocumentRef ref = new IdRef(docId);
                session.removeDocument(ref);

                proceed();

                if (explicitSave) {
                    session.save();
                }
            } finally {
                closeSession();
            }
        }
    }

    /**
     * Adds a document in the folder. Retries once.
     */
    public static class AddChildWork extends BaseWork {
        private static final long serialVersionUID = 1L;

        public List<Boolean> existList = new ArrayList<Boolean>();

        @Override
        public int getRetryCount() {
            return 1;
        }

        @Override
        public void work() throws Exception {
            initSession();
            ready();
            try {
                boolean exists = session.exists(new IdRef(docId));
                existList.add(Boolean.valueOf(exists));
                if (!exists) {
                    // after a retry, the folder is really gone
                    return;
                }

                proceed();

                DocumentModel doc = session.createDocumentModel("/folder",
                        "doc", "File");
                doc = session.createDocument(doc);
                if (explicitSave) {
                    session.save();
                }
            } catch (Exception cause) {
                if (!(cause instanceof ConcurrentUpdateException)) {
                    LogFactory.getLog(WorkTest.class).error("non concurrent error caught (no retry)", cause);
                } else {
                    LogFactory.getLog(WorkTest.class).info("concurrent error caught (should retry)", cause);
                }
                throw cause;
            } finally {
                closeSession();
            }
        }
    }

}
