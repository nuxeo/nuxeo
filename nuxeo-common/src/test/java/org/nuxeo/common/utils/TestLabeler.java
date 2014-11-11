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
import org.nuxeo.common.utils.i18n.Labeler;

public class TestLabeler extends TestCase {

    public void testMakeLabel() {
        String prefix = "some.prefix";
        Labeler l = new Labeler(prefix);

        assertEquals("some.prefix.item", l.makeLabel("item"));
        assertEquals("some.prefix.item", l.makeLabel("Item"));
    }

}
