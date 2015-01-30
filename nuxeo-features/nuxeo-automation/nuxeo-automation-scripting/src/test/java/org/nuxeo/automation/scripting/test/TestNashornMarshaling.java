/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.test;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.nuxeo.automation.scripting.MarshalingHelper;

/**
 * @since 7.2
 */
public class TestNashornMarshaling {

    public class Mapper {

        public Object callMe(ScriptObjectMirror params) {

            Map<String, Object> map = (Map<String, Object>) MarshalingHelper.unwrap(params);

            Integer p1 = (Integer) map.get("p1");
            String p2 = (String) map.get("p2");
            List<Object> p3 = (List<Object>) map.get("p3");

            Assert.assertEquals(3, p3.size());
            Map<String, Object> nested = (Map<String, Object>) p3.get(2);

            System.out.println(p1);
            System.out.println(p2);
            System.out.println(p3);
            System.out.println(nested);

            Map<String, Object> data = new HashMap<>();
            data.put("p1", "This is a string");
            data.put("p2", 2);
            List<String> l = new ArrayList<String>();
            l.add("A");
            l.add("B");
            l.add("C");
            data.put("p3", l);

            Map<String, Object> nested2 = new HashMap<>();
            nested2.put("a", "salut");
            nested2.put("b", "from java");
            data.put("p4", nested2);

            return MarshalingHelper.wrap(data);
        }

    }

    @Test
    public void test() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("Nashorn");
        Assert.assertNotNull(engine);

        Compilable compiler = (Compilable) engine;
        Assert.assertNotNull(compiler);

        InputStream stream = this.getClass().getResourceAsStream("/testScript.js");
        Assert.assertNotNull(stream);
        String js = IOUtils.toString(stream);

        CompiledScript compiled = compiler.compile(new StringReader(js));

        engine.put("mapper", new Mapper());

        compiled.eval(engine.getContext());

    }
}
