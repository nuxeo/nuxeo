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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.MarshalingHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

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

    @Inject AutomationScriptingService scriptingService;

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private PrintStream outStream;

    @Before
    public void setUpStreams() {
        outStream = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void cleanUpStreams() throws IOException {
        outContent.close();
        System.setOut(outStream);
    }

    @Test
    public void serviceShouldBeDeclared() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName(AutomationScriptingConstants.NASHORN_ENGINE);
        assertNotNull(engine);

        InputStream stream = this.getClass().getResourceAsStream("/checkWrapper.js");
        assertNotNull(stream);
        engine.eval(scriptingService.getJSWrapper());
        engine.eval(IOUtils.toString(stream));
        assertEquals("Hello\n", outContent.toString());
    }

    @Test
    public void testNashornWithCompile() throws Exception {
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

    @Ignore("for performance testing purpose")
    @Test
    public void testPerf() throws ScriptException {
        long start = System.currentTimeMillis();
        for(int i=0;i<500;i++) {
            scriptingService.run(scriptingService.getJSWrapper(), session);
        }
        long end = System.currentTimeMillis();
        System.err.println("DEBUG: Logic A toke " + (end - start) + " MilliSeconds");
    }

    protected String getScriptWithRandomContent(String content) {
        // change the content of the script !
        return "var t=" + System.currentTimeMillis() + content;
    }

    @Ignore("for performance testing purpose")
    @Test
    public void checkScriptingEngineCostAndIsolation() throws Exception {

        InputStream stream = this.getClass().getResourceAsStream("/QuickScript.js");
        assertNotNull(stream);
        String js = IOUtils.toString(stream);

        long t0 = System.currentTimeMillis();
        scriptingService.run(getScriptWithRandomContent(js), session);
        long t1 = System.currentTimeMillis();
        //System.err.println("Initial Exec = " + (t1-t0));


        t0 = System.currentTimeMillis();
        scriptingService.run(getScriptWithRandomContent(js), session);
        t1 = System.currentTimeMillis();
        //System.err.println("Second Exec = " + (t1-t0));

        int nbIter = 50;

        long t = t1-t0;
        for (int i = 0; i < nbIter; i++) {
            t0 = System.currentTimeMillis();
            scriptingService.run(getScriptWithRandomContent(js), session);
            t1 = System.currentTimeMillis();
            //System.err.println("Exec = " + (t1-t0));
            t+=t1-t0;
        }

        //System.err.println("AvgExec = " + (t/(nbIter + 1.0)));

        // now we check isolation

        stream = this.getClass().getResourceAsStream("/checkIsolation.js");
        assertNotNull(stream);
        String check = IOUtils.toString(stream);

        scriptingService.run(check, session);

        scriptingService.run(check, session);

        scriptingService.run("Document.Fetch=\"toto\";", session);

        scriptingService.run(check, session);
    }



    public class Mapper {

        public Object callMe(ScriptObjectMirror params) {

            Map<String, Object> map = (Map<String, Object>) MarshalingHelper
                    .unwrap(params);

            Integer p1 = (Integer) map.get("p1");
            String p2 = (String) map.get("p2");
            List<Object> p3 = (List<Object>) map.get("p3");

            assertEquals(3, p3.size());
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
    public void testIsolationScriptCtx() throws Exception {
        org.junit.Assert.assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/scriptCtxIsolation.js");
        org.junit.Assert.assertNotNull(stream);
        scriptingService.run(stream, session);
        assertEquals("[object Object]\n", outContent.toString());

        stream = this.getClass().getResourceAsStream("/scriptCtxIsolation.js");
        org.junit.Assert.assertNotNull(stream);
        scriptingService.run(stream, session);
        // Failing returning "[object Object]\n" + "toto\n"
        assertEquals("[object Object]\n" + "[object Object]\n", outContent.toString());
    }

}
