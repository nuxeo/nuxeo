/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URI;
import java.text.DateFormat;
import java.util.Date;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestLockingRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/Locking";

    protected static final String ACTION_LOCK = "lock";

    protected static final String ACTION_UNLOCK = "unlock";

    protected static final String ACTION_STATUS = "status";

    protected static final String ACTION_STATE = "state";

    protected static final DateFormat DATE_MEDIUM_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM);

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel root;

    protected DocumentModel doc;

    protected String username;

    protected static class HttpLock extends HttpRequestBase {

        public HttpLock(String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "LOCK";
        }
    }

    protected static class HttpUnlock extends HttpRequestBase {

        public HttpUnlock(String uri) {
            super();
            setURI(URI.create(uri));
        }

        @Override
        public String getMethod() {
            return "UNLOCK";
        }
    }

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        root = session.getRootDocument();
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "sometitle");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
        username = USERNAME;
    }

    @Override
    protected void setAuthorization(HttpUriRequest request) {
        super.setAuthorization(request, username);
    }

    protected Lock lockDoc() {
        Lock lock = session.setLock(doc.getRef());
        session.save();
        txFeature.nextTransaction();
        return lock;
    }

    @Test
    public void testLockWithMethod() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT;
        doTestLockOk(path, HttpLock::new);
    }

    @Test
    public void testLock() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_LOCK;
        doTestLockOk(path, HttpGet::new);
    }

    protected void doTestLockOk(String path, Function<String, HttpUriRequest> requestBuilder) throws Exception {
        String content = XML + "<document code=\"OK\" message=\"lock acquired on document " + doc.getId() + "\"/>";
        executeRequest(path, requestBuilder, content);
        // check now locked
        txFeature.nextTransaction();
        assertNotNull(session.getLockInfo(doc.getRef()));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testLockAlreadyLockedByYou() throws Exception {
        lockDoc();
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_LOCK;
        username = SecurityConstants.ADMINISTRATOR;
        String content = XML + "<document code=\"ALREADYLOCKEDBYYOU\" message=\"document " + doc.getId()
                + " is already locked by you\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testLockAlreadyLockedByOther() throws Exception {
        lockDoc();
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_LOCK;
        String content = XML + "<document code=\"ALREADYLOCKED\" message=\"document " + doc.getId()
                + " is already locked by Administrator\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testUnlock() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_UNLOCK;
        doTestUnlockOk(path, HttpGet::new);
    }

    @Test
    public void testUnlockWithMethod() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT;
        doTestUnlockOk(path, HttpUnlock::new);
    }

    @SuppressWarnings("deprecation")
    protected void doTestUnlockOk(String path, Function<String, HttpUriRequest> requestBuilder) throws Exception {
        lockDoc();
        String content = XML + "<document code=\"OK\" message=\"document " + doc.getId() + " unlocked\"/>";
        username = SecurityConstants.ADMINISTRATOR;
        executeRequest(path, requestBuilder, content);
        // check now unlocked
        txFeature.nextTransaction();
        assertNull(session.getLockInfo(doc.getRef()));
    }

    @Test
    public void testUnlockLockedByOther() throws Exception {
        lockDoc();
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_UNLOCK;
        String content = XML + "<document code=\"ALREADYLOCKED\" message=\"document " + doc.getId()
                + " is locked by Administrator\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testUnlockNotLocked() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_UNLOCK;
        String content = XML + "<document code=\"NOT LOCKED\" message=\"document " + doc.getId() + " is not locked\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testStatusNotLocked() throws Exception {
        doTestStatusNotLocked("/" + ACTION_STATUS);
    }

    @Test
    public void testStatusNotLockedDefaultAction() throws Exception {
        doTestStatusNotLocked("");
    }

    protected void doTestStatusNotLocked(String endPath) throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + endPath;
        String content = XML + "<document code=\"NOTLOCKED\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testStatusLocked() throws Exception {
        Lock lock = lockDoc();
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_STATUS;
        String lockInfo = lock.getOwner() + ':'
                + DATE_MEDIUM_FORMAT.format(new Date(lock.getCreated().getTimeInMillis()));
        String content = XML + "<document code=\"LOCKED\" message=\"" + lockInfo + "\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testStateNotLocked() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_STATE;
        String content = XML + "<document code=\"NOTLOCKED\" message=\"\"/>";
        executeRequest(path, content);
    }

    @Test
    public void testStateLocked() throws Exception {
        Lock lock = lockDoc();
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT + "/" + ACTION_STATE;
        String lockInfo = lock.getOwner() + '/' + ISODateTimeFormat.dateTime().print(new DateTime(lock.getCreated()));
        String content = XML + "<document code=\"LOCKED\" message=\"" + lockInfo + "\"/>";
        executeRequest(path, content);
    }

}
