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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author sfermigier
 */
public class TestTextTemplate extends TestCase {

    public void test1() {
        TextTemplate tt = new TextTemplate();
        tt.setVariable("var1", "value1");
        tt.setVariable("var2", "value2");
        tt.setVariable("var3", "value3");
        String text = tt.process("test ${var1} and ${var2} and ${var3}");
        assertEquals("test value1 and value2 and value3", text);
    }

    public void test2() {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("k1", "v1");
        TextTemplate tt = new TextTemplate(vars);

        assertEquals(vars, tt.getVariables());
        assertEquals("v1", tt.getVariable("k1"));

        tt.setVariable("k2", "v2");
        String text = tt.process("${k1}-${k2}");
        assertEquals("v1-v2", text);
    }

}
