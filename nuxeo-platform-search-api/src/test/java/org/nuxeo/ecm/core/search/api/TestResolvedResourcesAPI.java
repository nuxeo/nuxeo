/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestResolvedResourcesAPI.java 21705 2007-07-02 00:59:34Z janguenot $
 */

package org.nuxeo.ecm.core.search.api;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.impl.ResolvedResourcesImpl;

/**
 * Test resolved indexable resources API.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TestResolvedResourcesAPI extends TestCase {

    public void testResolvedResources() {
        ResolvedResources resources = new ResolvedResourcesImpl("id",
                null, null, null);
        assertEquals("id", resources.getId());
        assertEquals(0, resources.getIndexableResolvedResources().size());
        assertEquals(0, resources.getMergedIndexableData().size());
        assertEquals(0, resources.getCommonIndexableData().size());
        assertNull(resources.getACP());
    }

}
