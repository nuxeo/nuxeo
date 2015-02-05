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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.MarshalingHelper;
import org.nuxeo.automation.scripting.internals.ScriptRunner;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.automation.core" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.automation.scripting:OSGI-INF/automation-scripting-service.xml" })
public class TestCompileAndContext {

    @Inject
    CoreSession session;

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(null);
    }

    @Test
    public void serviceShouldBeDeclared() throws Exception {
        AutomationScriptingService automationScriptingService = Framework.getService(AutomationScriptingService.class);
        assertNotNull(automationScriptingService);

        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName(AutomationScriptingConstants.NASHORN_ENGINE);
        assertNotNull(engine);

        InputStream stream = this.getClass().getResourceAsStream("/checkWrapper.js");
        assertNotNull(stream);
        engine.eval(automationScriptingService.getJSWrapper());
        engine.eval(IOUtils.toString(stream));
        assertEquals("Hello\n", outContent.toString());
    }

    @Test
    public void testNashornPrecompilation() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName(AutomationScriptingConstants.NASHORN_ENGINE);
        assertNotNull(engine);

        Compilable compiler = (Compilable) engine;
        assertNotNull(compiler);

        InputStream stream = this.getClass().getResourceAsStream("/testScript.js");
        assertNotNull(stream);
        String js = IOUtils.toString(stream);

        CompiledScript compiled = compiler.compile(new StringReader(js));

        engine.put("mapper", new Mapper());

        compiled.eval(engine.getContext());
        assertEquals("1\n" +
                "str\n" +
                "[1, 2, {a=1, b=2}]\n" +
                "{a=1, b=2}\n" +
                "This is a string\n" +
                "This is a string\n" +
                "2\n" +
                "[A, B, C]\n" +
                "{a=salut, b=from java}\n" +
                "done\n", outContent.toString());

    }

    public class Mapper {

        public Object callMe(ScriptObjectMirror params) {

            Map<String, Object> map = (Map<String, Object>) MarshalingHelper
                    .unwrap(params);

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

}
