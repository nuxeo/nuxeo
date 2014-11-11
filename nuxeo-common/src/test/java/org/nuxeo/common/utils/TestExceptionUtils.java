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

package org.nuxeo.common.utils;

import junit.framework.TestCase;

public class TestExceptionUtils extends TestCase {

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testExceptionCause() {
        Exception e1 = new Exception("root");
        Exception e2 = new Exception("child1", e1);
        Exception e3 = new Exception("child2", e2);

        assertEquals("root", ExceptionUtils.getRootCause(e3).getMessage());
    }
}
