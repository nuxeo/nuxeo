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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.storage.sql.TXSQLRepositoryTestCase;

public class TestUnrestrictedSessionRunnerJCA extends TXSQLRepositoryTestCase {

    public void testUnrestrictedPropertySetter() throws Exception {
        closeSession();
        session = openSessionAs("bob");
        TestUnrestrictedSessionRunner.seeDocCreatedByUnrestricted(session);
    }

    public void testUnrestrictedSessionSeesDocCreatedBefore() throws Exception {
        closeSession();
        session = openSessionAs("Administrator");
        TestUnrestrictedSessionRunner.unrestrictedSeesDocCreatedBefore(session);
    }
}
