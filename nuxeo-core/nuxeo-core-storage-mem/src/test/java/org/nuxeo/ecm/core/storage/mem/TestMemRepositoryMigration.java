/*
 * Copyright (c) 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mem;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSRepository;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestMemRepositoryMigration extends MemRepositoryTestCase {

    @Test
    public void testListVsString() throws Exception {
        // create a doc
        DocumentModel doc = session.createDocumentModel("/", "domain", "MyDocType");
        doc = session.createDocument(doc);
        String id = doc.getId();
        session.save();

        TransactionHelper.commitOrRollbackTransaction();
        waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        // change data
        StateDiff diff = new StateDiff();
        diff.put("dc:subjects", "a"); // put a string instead of an array
        diff.put("dc:title", new String[] { "aa", "bb" }); // put an array instead of a string
        changeDoc(id, diff);

        // check that we don't crash on read
        doc = session.getDocument(doc.getRef());
        assertEquals(Arrays.asList("a"), Arrays.asList((Object[]) doc.getPropertyValue("dc:subjects")));
        assertEquals("aa", doc.getPropertyValue("dc:title"));
        assertEquals(Collections.emptyList(), Arrays.asList((Object[]) doc.getPropertyValue("dc:contributors")));
    }

    // change data in the database to make it like we just migrated the fields from different types
    protected void changeDoc(String id, StateDiff diff) throws Exception {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(session.getRepositoryName());
        ((DBSRepository) repository).updateState(id, diff);
    }

}
