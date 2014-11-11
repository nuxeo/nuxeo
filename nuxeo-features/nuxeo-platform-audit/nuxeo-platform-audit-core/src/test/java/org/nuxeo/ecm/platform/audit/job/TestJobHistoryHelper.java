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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.job;

import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.job.JobHistoryHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestJobHistoryHelper extends NXRuntimeTestCase {


    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.persistence");
        deployBundle("org.nuxeo.ecm.platform.audit");
        deployBundle("org.nuxeo.ecm.platform.audit.tests");
        deployTestContrib("org.nuxeo.ecm.platform.audit.tests", "nxaudit-tests.xml");
        fireFrameworkStarted();
    }

    public void testLogger() throws Exception {

        StringBuffer query = new StringBuffer("from LogEntry log where ");
        query.append(" log.category='");
        query.append("MyExport");
        query.append("'  ORDER BY log.eventDate DESC");

        AuditReader reader = Framework.getService(AuditReader.class);

        List result = reader.nativeQuery(query.toString(), 1, 1);
        assertEquals(0, result.size());

        JobHistoryHelper helper = new JobHistoryHelper("MyExport");

        helper.logJobStarted();
        helper.logJobFailed("some error");
        helper.logJobEnded();

        result = reader.nativeQuery(query.toString(), 1, 10);
        assertEquals(3, result.size());

    }

    public void testLoggerHelper() throws Exception {

        JobHistoryHelper helper = new JobHistoryHelper("MyExport2");


        helper.logJobStarted();

        Date exportDate = helper.getLastSuccessfulRun();
        assertNull(exportDate);

        helper.logJobFailed("some other error");

        exportDate = helper.getLastSuccessfulRun();
        assertNull(exportDate);

        helper.logJobEnded();

        exportDate = helper.getLastSuccessfulRun();
        assertNotNull(exportDate);

        Thread.sleep(3000);
        long t0 = System.currentTimeMillis();

        exportDate = helper.getLastSuccessfulRun();
        long loggedT0 = exportDate.getTime();
        assertTrue(loggedT0 < t0);


        helper.logJobEnded();
        exportDate = helper.getLastSuccessfulRun();
        long loggedT1 = exportDate.getTime();

        assertTrue(loggedT1-loggedT0>= 3000);
    }

}
