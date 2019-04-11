/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;
import com.google.inject.name.Named;

@RunWith(FeaturesRunner.class)
@Features(CoreDirectoryFeature.class)
public class TestCoreDirectoryReadOnly {

    @Inject
    @Named(value = CoreDirectoryFeature.CORE_DIRECTORY_NAME)
    protected Directory coreDir;

    protected Session dirSystemReadOnlySession = null;

    @Before
    public void setUp() throws Exception {
        ((CoreDirectory) coreDir).getDescriptor().setReadOnly(true);
        dirSystemReadOnlySession = coreDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        dirSystemReadOnlySession.close();
    }

    @Test
    public void testReadOnlyOnGetEntry() throws Exception {
        assertTrue(dirSystemReadOnlySession.isReadOnly());
        DocumentModel entry = dirSystemReadOnlySession.getEntry(CoreDirectoryInit.DOC_ID_USER1);
        assertNotNull(entry);
        assertTrue(BaseSession.isReadOnlyEntry(entry));
    }

    @Test
    public void testReadOnlyEntryInQueryResults() throws Exception {
        Map<String, String> orderBy = new HashMap<>();
        orderBy.put(TestCoreDirectory.UID_FIELD, "asc");
        DocumentModelComparator comp = new DocumentModelComparator(orderBy);

        Map<String, Serializable> filter = new HashMap<>();

        DocumentModelList results = dirSystemReadOnlySession.query(filter);
        Collections.sort(results, comp);
        assertTrue(BaseSession.isReadOnlyEntry(results.get(0)));
        assertTrue(BaseSession.isReadOnlyEntry(results.get(1)));

    }

}
