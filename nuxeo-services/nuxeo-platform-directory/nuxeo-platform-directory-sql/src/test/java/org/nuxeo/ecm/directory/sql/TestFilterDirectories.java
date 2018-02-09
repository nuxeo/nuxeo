/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.sql.tests:filterDirectoryContrib.xml")
public class TestFilterDirectories {

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testFilterDirectory() throws Exception {
        Session unfiltredSession = directoryService.open("unfiltredTestDirectory");
        assertNotNull(unfiltredSession);
        assertEquals(5, unfiltredSession.getEntries().size());
        assertNotNull(unfiltredSession.getEntry("1"));
        assertNotNull(unfiltredSession.getEntry("2"));
        assertNotNull(unfiltredSession.getEntry("5"));

        Map<String, Serializable> queryFilter = new HashMap<String, Serializable>();
        queryFilter.put("lang", "en");
        assertEquals(2, unfiltredSession.query(queryFilter).size());

        Session filtredSession = directoryService.open("filtredTestDirectory");
        assertNotNull(filtredSession);
        assertEquals(2, filtredSession.getEntries().size());
        assertNotNull(filtredSession.getEntry("1"));
        assertNull(filtredSession.getEntry("2"));
        assertNull(filtredSession.getEntry("5"));
        assertEquals(1, filtredSession.query(queryFilter).size());
    }

}
