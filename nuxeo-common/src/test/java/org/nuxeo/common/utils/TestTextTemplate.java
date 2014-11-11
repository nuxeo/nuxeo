/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author sfermigier
 */
public class TestTextTemplate {

    @Test
    public void test1() {
        TextTemplate tt = new TextTemplate();
        tt.setVariable("var1", "value1");
        tt.setVariable("var2", "value2");
        tt.setVariable("var3", "value3");
        String text = tt.process("test ${var1} and ${var2} and ${var3}");
        assertEquals("test value1 and value2 and value3", text);
    }

    @Test
    public void test2() {
        Properties vars = new Properties();
        vars.setProperty("k1", "v1");
        TextTemplate tt = new TextTemplate(vars);

        assertEquals(vars, tt.getVariables());
        assertEquals("v1", tt.getVariable("k1"));

        tt.setVariable("k2", "v2");
        String text = tt.process("${k1}-${k2}");
        assertEquals("v1-v2", text);
    }

}
