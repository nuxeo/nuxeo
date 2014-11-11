/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests memory leak by repeating operations.
 *
 * @author Florent Guillaume
 */
public class TestSQLBackendMemoryLeak extends TestCase {

    private static final Log log = LogFactory.getLog(TestSQLBackendMemoryLeak.class);

    public static final int ITERATIONS = 20;

    public void testRepeat() throws Exception {
        for (int i = 1; i <= ITERATIONS; i++) {
            log.info("\n\n\n\n\n---------------------------------- Iteration " +
                    i);
            doit();
            System.gc();
            Thread.sleep(1);
            System.gc();
            log.info("----- End Iteration " + i);
            log.info("-----   Used mem: " + Runtime.getRuntime().totalMemory());
        }
    }

    public void doOne(String name) {
        log.warn("----- " + name);
        new TestSQLBackend(name).run();
        System.gc();
        log.warn("----- used mem: " + Runtime.getRuntime().totalMemory());
    }

    private void doit() {
        doOne("testRootNode");
        doOne("testChildren");
        doOne("testBasics");
        doOne("testPropertiesSameName");
        doOne("testBinary");
        doOne("testACLs");
        doOne("testCrossSessionInvalidations");
        doOne("testMove");
        doOne("testCopy");
        doOne("testVersioning");
        doOne("testProxies");
        doOne("testDelete");
    }

}
