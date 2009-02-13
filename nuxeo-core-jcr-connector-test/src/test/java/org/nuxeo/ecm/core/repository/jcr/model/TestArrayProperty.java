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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.model;

import java.util.Arrays;
import java.util.Calendar;

import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.jcr.testing.CoreJCRConnectorTestConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;

/**
 * @author Anahide Tchertchian
 *
 */
public class TestArrayProperty extends RepositoryTestCase {

    private Session session;

    private Document root;

    private Document dateDoc;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreJCRConnectorTestConstants.TESTS_BUNDLE,
                "test-core-types.xml");
        // creating the session
        session = getSession();
        root = session.getRootDocument();
        // add test date doc
        dateDoc = root.addChild("dateDocument", "TestDocument");
    }

    // NXP-2454
    public void testDateArrayProperty() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(2008, 6, 5);
        Calendar[] calendarArray = { date };
        dateDoc.setPropertyValue("dateArray", calendarArray);

        assertTrue(Arrays.equals(calendarArray,
                (Object[]) dateDoc.getPropertyValue("dateArray")));
    }

}
