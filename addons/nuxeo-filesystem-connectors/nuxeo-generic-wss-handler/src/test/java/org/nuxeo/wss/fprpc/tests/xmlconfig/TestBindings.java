/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.fprpc.tests.xmlconfig;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.FilterBindingResolver;

public class TestBindings {

    @Test
    public void testBindings() throws Exception {

        FilterBindingConfig config = FilterBindingResolver.getBinding("/_vti_inf.html");
        assertNotNull(config);
        assertEquals("VtiHandler", config.getTargetService());
        assertEquals("", config.getSiteName());

        config = FilterBindingResolver.getBinding("/_vti_inf.html?XXX=YYYY");
        assertNotNull(config);
        assertEquals("VtiHandler", config.getTargetService());
        assertEquals("", config.getSiteName());

        config = FilterBindingResolver.getBinding("/server/_vti_inf.html?UUUU=VVVV");
        assertNotNull(config);
        assertEquals("VtiHandler", config.getTargetService());
        assertEquals("/server", config.getSiteName());

    }
}
