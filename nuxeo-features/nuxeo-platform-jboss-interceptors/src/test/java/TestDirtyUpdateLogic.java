/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     slacoin
 *
 * $Id$
 */
import java.util.Calendar;
import java.util.ConcurrentModificationException;

import org.nuxeo.common.DirtyUpdateInvokeBridge;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

public class TestDirtyUpdateLogic extends SQLRepositoryTestCase {

    protected DocumentRef ref;

    protected DocumentModel doc;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        openSession();
        DocumentModel doc = session.createDocumentModel("/", "test", "Note");
        doc.getProperty("dc:title").setValue(session.getSessionId());
        doc = session.createDocument(doc);
        ref = doc.getRef();
        session.save();
    }

    protected void updateDoc() throws PropertyException, ClientException {
        CoreSession repo = openSessionAs(SecurityConstants.ADMINISTRATOR);
        doc = repo.getDocument(ref);
        doc.getProperty("dc:title").setValue("session " + repo.getSessionId());
        doc = repo.saveDocument(doc);
        repo.save();
        closeSession(repo);
    }

    public void testConcurrentUpdate() throws ClientException {
        Long timeref1 = Calendar.getInstance().getTimeInMillis();
        updateDoc();
        DirtyUpdateInvokeBridge.putTagInThreadContext(timeref1);
        boolean gotError = false;
        try {
            updateDoc();
        } catch (ConcurrentModificationException e) {
            gotError = true;
        }
        assertTrue("has not got error", gotError);
    }

    public void testSelfUpdate() throws ClientException {
        Long timeref1 = Calendar.getInstance().getTimeInMillis();
        DirtyUpdateInvokeBridge.putTagInThreadContext(timeref1);
        boolean gotError = false;
        try {
            updateDoc();
        } catch (ConcurrentModificationException e) {
            gotError = true;
        }
        assertFalse("has got error", gotError);
    }

}
