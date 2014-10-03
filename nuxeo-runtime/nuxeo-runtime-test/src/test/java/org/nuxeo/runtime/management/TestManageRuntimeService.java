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

package org.nuxeo.runtime.management;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestManageRuntimeService extends NXRuntimeTestCase {

    private final RuntimeServiceMBeanAdapter adapterUnderTest = new RuntimeServiceMBeanAdapter();

    @Test
    public void testPrint() {
        Set<String> resolvedComponents = adapterUnderTest.getResolvedComponents();
        assertNotNull(resolvedComponents);
        assertTrue(resolvedComponents.size() > 0);
    }

}
