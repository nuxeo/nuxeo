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
 * $Id: TestTypeService.java 26932 2007-11-07 15:05:49Z gracinet $
 */

package org.nuxeo.ecm.core.schema;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
 */
public class TestTypeService extends NXRuntimeTestCase {

    private TypeService ts;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("nuxeo-core-schema", "OSGI-INF/SchemaService.xml");
        ts = (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
    }

    public void testTypeService() {
        assertNull(ts.getConfiguration());
        assertNotNull(ts.getSchemaLoader());
        assertNotNull(ts.getTypeManager());
    }

}
