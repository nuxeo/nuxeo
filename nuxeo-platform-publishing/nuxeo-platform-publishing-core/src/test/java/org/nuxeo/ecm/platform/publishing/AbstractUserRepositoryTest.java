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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;

/**
 * @author arussel
 *
 */
public class AbstractUserRepositoryTest extends RepositoryOSGITestCase {

    public static final String TEST_BUNDLE = "org.nuxeo.ecm.platform.publishing.tests";
    public static final String PUB_BUNDLE = "org.nuxeo.ecm.platform.publishing";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployBundle("org.nuxeo.ecm.directory.sql");

        deployBundle(PUB_BUNDLE);
        deployBundle(TEST_BUNDLE);
    }

    public void testTest() {
        assertTrue(true);
    }
}
