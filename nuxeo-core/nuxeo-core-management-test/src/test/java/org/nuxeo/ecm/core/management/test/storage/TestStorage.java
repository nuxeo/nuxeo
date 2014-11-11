/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.management.test.storage;

import org.nuxeo.ecm.core.management.test.CoreManagementTestCase;
import org.nuxeo.ecm.core.management.test.FakeDocumentStoreHandler;

public class TestStorage extends CoreManagementTestCase {

    public void testRegistration() {
        assertNotNull("handler is not contributed",
                FakeDocumentStoreHandler.testInstance);
        assertNotNull("handler is not invoked",
                FakeDocumentStoreHandler.testInstance.repositoryName);
        assertEquals("configuration is not contributed", "test",
                FakeDocumentStoreHandler.testInstance.repositoryName);
    }
}
