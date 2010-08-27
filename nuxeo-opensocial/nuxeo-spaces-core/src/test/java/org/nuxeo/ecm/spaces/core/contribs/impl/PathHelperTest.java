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

package org.nuxeo.ecm.spaces.core.contribs.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PathHelperTest {

    @Test
    public void getParentPath() throws Exception {
        String path = "/default-domain/workspaces/galaxy/intralm";
        assertEquals("/default-domain/workspaces/galaxy",
                SingleDocSpaceProvider.getParentPath(path));
        assertEquals("intralm", SingleDocSpaceProvider.getDocName(path));
        assertEquals("/", SingleDocSpaceProvider.getParentPath("/home"));
    }
}
