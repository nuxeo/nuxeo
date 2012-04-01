/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.annotated;

import org.apache.commons.logging.Log;
import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.openqa.jetty.log.LogFactory;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author matic
 *
 */
public class LoadDummyAnnotated extends NXRuntimeTestCase {

    protected static final Log log = LogFactory.getLog(LoadDummyAnnotated.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.runtime.test.tests", "dummy.xml");
        new DummyAnnotatedServiceProvider().installSelf();
    }

    @After
    public void tearDown() throws Exception {
        printSimon(SimonManager.getRootSimon());
    }

    protected void printSimon(Simon s) {
        log.warn(s);
        for (Simon c:s.getChildren()) {
            printSimon(c);
        }
    }

    @Test
    public void testLoadService() {
        generateCalls( "annotated", Framework.getLocalService(DummyAnnotated.class));
    }

    @Test
    public void testloadPojo() {
        generateCalls("pojo", new DummyImpl());
    }

    public void generateCalls(String name, DummyAnnotated da) {
        Stopwatch sw = SimonManager.getStopwatch(name);
        for(int i = 0; i < 1000000; ++i) {
            Split s = sw.start();
            da.dummy();
            s.stop();
        } 
    }

}
