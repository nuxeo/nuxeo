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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.scripting;

import junit.framework.TestCase;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings({"UnnecessaryBoxing"})
public class TestEngines extends TestCase {

    private ScriptEngineManager factory = new ScriptEngineManager();

    public void testJRuby() throws ScriptException {
        // Create a JRuby engine.
        ScriptEngine engine = factory.getEngineByName("jruby");

        // Evaluate JRuby code from string.
        assertEquals("Hello", engine.eval("'Hello'"));
        assertEquals(Long.valueOf(3), engine.eval("1 + 2"));

        // Doesn't work.
        //engine.eval("x = 1");
        //assertEquals(Integer.valueOf(1), engine.eval("x"));
        //assertEquals(Integer.valueOf(1), engine.get("x"));
    }

    public void testJS() throws ScriptException {
        // Create a JS engine.
        ScriptEngine engine = factory.getEngineByName("js");

        // Evaluate JS code from string.
        assertEquals("Hello", engine.eval("'Hello'"));
        assertEquals(Integer.valueOf(3), engine.eval("1 + 2"));
        engine.eval("var x = 1 + 2;");
        assertEquals(Integer.valueOf(3), engine.eval("x"));
        assertEquals(Integer.valueOf(3), engine.get("x"));
    }

    public void testJython() throws ScriptException {
        // Create a Python engine.
        ScriptEngine engine = factory.getEngineByName("jython");

        // Evaluate Jython code from string.
        // This doesn't work for Jython
        //assertEquals("Hello", engine.eval("'Hello'"));
        engine.eval("s = 'Hello'");
        assertEquals("Hello", engine.get("s"));
        engine.eval("x = 1 + 2");
        assertEquals(Integer.valueOf(3), engine.get("x"));
    }

    public void testJEXL() throws ScriptException {
        // Create a JEXL engine.
        ScriptEngine engine = factory.getEngineByName("jexl");

        // Evaluate JEXL code from string.
        assertEquals("Hello", engine.eval("\"Hello\""));
        assertEquals(Integer.valueOf(3), engine.eval("3"));
    }

    public void testGroovy() throws ScriptException {
        // Create a Groovy engine.
        ScriptEngine engine = factory.getEngineByName("groovy");

        // Evaluate Groovy code from string.
        assertEquals("Hello", engine.eval("\"Hello\""));
        assertEquals(Integer.valueOf(3), engine.eval("1 + 2"));
        engine.eval("s = \"Hello\"");
        assertEquals("Hello", engine.get("s"));
        engine.eval("x = 1 + 2");
        assertEquals(Integer.valueOf(3), engine.get("x"));
    }

    // BSH broken seemingly
    public void xxxtestBSH() throws ScriptException {
        // Create a BSH engine.
        ScriptEngine engine = factory.getEngineByName("bsh");

        // Evaluate BSH code from string.
        assertEquals("Hello", engine.eval("\"Hello\""));
    }

}
