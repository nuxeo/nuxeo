/*
 * (C) Copyright 2010-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.directory.sql.tests:intIdDirectory-contrib.xml")
public class TestIntIdField {

    @Inject
    protected DirectoryService directoryService;

    @SuppressWarnings("boxing")
    @Test
    public void testIntIdDirectory() throws Exception {
        try (Session session = directoryService.open("testIdDirectory")) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", 1);
            map.put("label", "toto");
            DocumentModel entry = session.createEntry(map);
            assertNotNull(entry);

            map.put("id", 2);
            map.put("label", "titi");
            DocumentModel entry2 = session.createEntry(map);
            assertNotNull(entry2);

            assertNotNull(session.getEntry("1"));
            assertNotNull(session.getEntry("2"));
            assertNull(session.getEntry("3"));
        }
    }

}
