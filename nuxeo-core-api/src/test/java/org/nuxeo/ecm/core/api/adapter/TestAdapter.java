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
 * $Id: TestAdapter.java 21546 2007-06-28 11:46:32Z sfermigier $
 */

package org.nuxeo.ecm.core.api.adapter;

import junit.framework.TestCase;

public class TestAdapter extends TestCase {

    @SuppressWarnings({"InterfaceNeverImplemented"})
    private interface AnInterface {
    }

    public void test() {
        DocumentAdapterService das = new DocumentAdapterService();
        das.activate(null);

        DocumentAdapterDescriptor dad = new DocumentAdapterDescriptor();
        dad.setInterface(AnInterface.class);

        das.registerAdapterFactory(dad);
        assertEquals(dad, das.getAdapterDescriptor(AnInterface.class));

        das.unregisterAdapterFactory(AnInterface.class);
        das.deactivate(null);
    }

}
