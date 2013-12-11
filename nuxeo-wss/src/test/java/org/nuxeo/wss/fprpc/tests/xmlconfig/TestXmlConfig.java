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

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.XmlConfigHandler;

public class TestXmlConfig {

    @Test
    public void testParsing() throws Exception {

        List<FilterBindingConfig> bindings = XmlConfigHandler.getConfigEntries();
        assertTrue(bindings.size()>0);

        FilterBindingConfig binding = bindings.get(0);
        assertEquals("(.*)/_vti_inf.html.*",binding.getUrl());
        assertEquals("GET",binding.getRequestType());
        assertEquals("VtiHandler",binding.getTargetService());
        assertEquals(null,binding.getRedirectURL());



    }
}
