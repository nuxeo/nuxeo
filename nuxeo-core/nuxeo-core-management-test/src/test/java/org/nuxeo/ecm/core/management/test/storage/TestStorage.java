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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.management.test.FakeDocumentStoreHandler;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.runtime.management", //
        "org.nuxeo.ecm.core.management", //
        "org.nuxeo.ecm.core.management.test" })
public class TestStorage {

    @Test
    public void testRegistration() {
        assertNotNull("handler is not contributed",
                FakeDocumentStoreHandler.testInstance);
        assertNotNull("handler is not invoked",
                FakeDocumentStoreHandler.testInstance.repositoryName);
        assertEquals("configuration is not contributed", "test",
                FakeDocumentStoreHandler.testInstance.repositoryName);
    }
}
