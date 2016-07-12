/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.api.AutomationScriptingConstants;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.ScriptObjectMirrors;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.automation.scripting:OSGI-INF/automation-scripting-service.xml" })
public class TestCompileAndContext {

    @Inject
    CoreSession session;

    @Inject
    AutomationScriptingService scriptingService;

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
        assertEquals("Hello" + System.lineSeparator(), outContent.toString());
    }

    @Test
    public void testNashornWithCompile() throws Exception {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName(AutomationScriptingConstants.NASHORN_ENGINE);
        assertNotNull(engine);

        Compilable compiler = (Compilable) engine;
        assertNotNull(compiler);

        InputStream stream = this.getClass().getResourceAsStream("/testScript" + ".js");
        assertNotNull(stream);
        String js = IOUtils.toString(stream);

        CompiledScript compiled = compiler.compile(new StringReader(js));

        engine.put("mapper", new Mapper());

        compiled.eval(engine.getContext());
        assertEquals(
                "1" + System.lineSeparator() + "str" + System.lineSeparator() + "[1, 2, {a=1, b=2}]"
                        + System.lineSeparator() + "{a=1, b=2}" + System.lineSeparator() + "This is a string"
                        + System.lineSeparator() + "This is a string" + System.lineSeparator() + "2"
                        + System.lineSeparator() + "[A, B, C]" + System.lineSeparator() + "{a=salut, b=from java}"
                        + System.lineSeparator() + "done" + System.lineSeparator(), outContent.toString());
    }

    @Ignore("for performance testing purpose")
    @Test
    public void testPerf() throws ScriptException, OperationException {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            scriptingService.run(scriptingService.getJSWrapper(), session);
        }
        long end = System.currentTimeMillis();
        System.err.println("DEBUG: Logic A toke " + (end - start) + " " + "MilliSeconds");
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

        // long t0 = System.currentTimeMillis();
        scriptingService.run(getScriptWithRandomContent(js), session);
        // long t1 = System.currentTimeMillis();
        // System.err.println("Initial Exec = " + (t1-t0));

        // t0 = System.currentTimeMillis();
        scriptingService.run(getScriptWithRandomContent(js), session);
        // t1 = System.currentTimeMillis();
        // System.err.println("Second Exec = " + (t1-t0));

        int nbIter = 50;

        // long t = t1 - t0;
        for (int i = 0; i < nbIter; i++) {
            // t0 = System.currentTimeMillis();
            scriptingService.run(getScriptWithRandomContent(js), session);
            // t1 = System.currentTimeMillis();
            // System.err.println("Exec = " + (t1-t0));
            // t += t1 - t0;
        }

        // System.err.println("AvgExec = " + (t/(nbIter + 1.0)));

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

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) ScriptObjectMirrors.unwrap(params);

            Integer p1 = (Integer) map.get("p1");
            String p2 = (String) map.get("p2");
            @SuppressWarnings("unchecked")
            List<Object> p3 = (List<Object>) map.get("p3");

            assertEquals(3, p3.size());
            @SuppressWarnings("unchecked")
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

            return ScriptObjectMirrors.wrap(data);
        }

    }

    @Test
    public void testIsolationScriptCtx() throws Exception {
        org.junit.Assert.assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/scriptCtxIsolation.js");
        org.junit.Assert.assertNotNull(stream);
        scriptingService.run(stream, session);
        assertEquals("[object Object]" + System.lineSeparator(), outContent.toString());

        stream = this.getClass().getResourceAsStream("/scriptCtxIsolation.js");
        org.junit.Assert.assertNotNull(stream);
        scriptingService.run(stream, session);
        // Failing returning "[object Object]\n" + "toto\n"
        assertEquals("[object Object]" + System.lineSeparator() + "[object " + "Object]" + System.lineSeparator(),
                outContent.toString());
    }

    @Test
    public void testAutomationCtxSharing() throws Exception {
        org.junit.Assert.assertNotNull(scriptingService);

        InputStream stream = this.getClass().getResourceAsStream("/shareAutomationContext.js");
        org.junit.Assert.assertNotNull(stream);
        scriptingService.run(stream, session);
        assertEquals("OK" + System.lineSeparator(), outContent.toString());
    }

}
