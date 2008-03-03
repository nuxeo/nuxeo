/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.repository.jcr.model;

import java.math.BigDecimal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * Test the 'system' properties of a document.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class TestSystemSchema extends RepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestSystemSchema.class);

    Document doc;

    Session session;

    @Override
    protected void setUp() throws Exception {
        // do nothing
    }

    @Override
    protected void tearDown() throws Exception {
        // do nothing
    }

    protected void start() throws Exception {
        super.setUp();
    }

    protected void shutdown() throws Exception {
        super.tearDown();
    }

    public void testProps() throws Exception {
        start();

        try {

            session = getRepository().getSession(null);
            doc = session.getRootDocument().addChild("mydoc", "File");

            // set String prop
            doc.setSystemProp("randomNameProperty", "Aaa");

            String val = doc.getSystemProp("randomNameProperty", String.class);
            log.info(val);
            assertEquals("Aaa", val);

            // set Integer prop
            doc.setSystemProp("randomNameProperty", 123);

            Long val2 = doc.getSystemProp("randomNameProperty",
                    Long.class);
            assertEquals(123, val2.intValue());

            BigDecimal notSupported = new BigDecimal(123);
            try {
                doc.setSystemProp("randomNameProperty", notSupported);
                fail("type should not be supported");
            } catch (DocumentException e) {
                // ok
            }

        } finally {
            shutdown();
        }
    }

}
