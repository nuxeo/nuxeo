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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.automation.scripting.AutomationScriptingFeature;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.internals.ScriptObjectMirrors;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.common.base.Charsets;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationScriptingFeature.class)
public class TestCompileAndContext {

    @Inject
    CoreSession session;

    @Inject
    AutomationScriptingService pool;

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
        try (AutomationScriptingService.Session ref = pool.get(session)) {
            try (InputStream stream = this.getClass().getResourceAsStream("/checkWrapper.js")) {
                assertNotNull(stream);
                ref.run(stream);
            }
        }
    }

    @Test
    public void testNashornWithCompile() throws Exception {
        try (AutomationScriptingService.Session ref = pool.get(session)) {

            Compilable compiler = ref.adapt(Compilable.class);
            assertNotNull(compiler);

            try (InputStream stream = this.getClass().getResourceAsStream("/testScript.js")) {
                assertNotNull(stream);
                String js = IOUtils.toString(stream);

                CompiledScript compiled = compiler.compile(new StringReader(js));

                final ScriptContext context = ref.adapt(ScriptContext.class);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                context.setWriter(new OutputStreamWriter(output));
                context.setAttribute("mapper", new Mapper(context), ScriptContext.ENGINE_SCOPE);
                compiled.eval(context);
                assertEquals(
                        "1" + System.lineSeparator() + "str" + System.lineSeparator() + "[1, 2, {a=1, b=2}]"
                                + System.lineSeparator() + "{a=1, b=2}" + System.lineSeparator() + "This is a string"
                                + System.lineSeparator() + "This is a string" + System.lineSeparator() + "2"
                                + System.lineSeparator() + "[A, B, C]" + System.lineSeparator()
                                + "{a=salut, b=from java}" + System.lineSeparator() + "done" + System.lineSeparator(),
                        output.toString());

            }
        }

    }

    @Ignore("for performance testing purpose")
    @Test
    public void testPerf() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 500; i++) {
            try (AutomationScriptingService.Session ref = pool.get(session)) {
                ;
            }
        }
        long end = System.currentTimeMillis();
        System.err.println("DEBUG: Logic A toke " + (end - start) + " " + "MilliSeconds");
    }

    protected InputStream getScriptWithRandomContent(String content) {
        // change the content of the script !
        return new ByteArrayInputStream(("var t=" + System.currentTimeMillis() + content).getBytes(Charsets.UTF_8));
    }

    @Ignore("for performance testing purpose")
    @Test
    public void checkScriptingEngineCostAndIsolation() throws Exception {
        try (AutomationScriptingService.Session session = pool.get(this.session)) {
            try (InputStream stream = this.getClass().getResourceAsStream("/QuickScript.js")) {
                assertNotNull(stream);
                String js = IOUtils.toString(stream);

                // long t0 = System.currentTimeMillis();
                session.run(getScriptWithRandomContent(js));
                // long t1 = System.currentTimeMillis();
                // System.err.println("Initial Exec = " + (t1-t0));

                // t0 = System.currentTimeMillis();
                session.run(getScriptWithRandomContent(js));
                // t1 = System.currentTimeMillis();
                // System.err.println("Second Exec = " + (t1-t0));

                int nbIter = 50;

                // long t = t1 - t0;
                for (int i = 0; i < nbIter; i++) {
                    // t0 = System.currentTimeMillis();
                    session.run(getScriptWithRandomContent(js));
                    // t1 = System.currentTimeMillis();
                    // System.err.println("Exec = " + (t1-t0));
                    // t += t1 - t0;
                }

                // System.err.println("AvgExec = " + (t/(nbIter + 1.0)));

                // now we check isolation

                session.run(this.getClass().getResourceAsStream("/checkIsolation.js"));

                session.run(this.getClass().getResourceAsStream("/checkIsolation.js"));

                session.run(new ByteArrayInputStream("Document.Fetch=\"toto\";".getBytes(Charsets.UTF_8)));

                session.run(this.getClass().getResourceAsStream("/checkIsolation.js"));
            }
        }
    }

    public class Mapper {

        final PrintWriter out;

        Mapper(ScriptContext context) {
            out = new PrintWriter(context.getWriter());
        }

        final public Object callMe(ScriptObjectMirror params) throws IOException {

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) ScriptObjectMirrors.unwrap(params);

            Integer p1 = (Integer) map.get("p1");
            String p2 = (String) map.get("p2");
            @SuppressWarnings("unchecked")
            List<Object> p3 = (List<Object>) map.get("p3");

            assertEquals(3, p3.size());
            @SuppressWarnings("unchecked")
            Map<String, Object> nested = (Map<String, Object>) p3.get(2);

            out.println(p1);
            out.println(p2);
            out.println(p3);
            out.println(nested);

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

        try (AutomationScriptingService.Session scripting1 = pool.get(session)) {
            ScriptObjectMirror mirror1 = (ScriptObjectMirror) scripting1.adapt(ScriptContext.class)
                    .getAttribute("nashorn.global");
            try (AutomationScriptingService.Session scripting2 = pool.get(session)) {
                ScriptObjectMirror mirror2 = (ScriptObjectMirror) scripting2.adapt(ScriptContext.class)
                        .getAttribute("nashorn.global");
                assertNotEquals(mirror1, mirror2);
            }
        }

    }

    @Test
    public void testAutomationCtxSharing() throws Exception {

        try (InputStream stream = this.getClass().getResourceAsStream("/shareAutomationContext.js")) {
            org.junit.Assert.assertNotNull(stream);
            try (AutomationScriptingService.Session scripting = pool.get(session)) {
                Object result = scripting.run(stream);
                assertEquals("OK", result);
            }
        }
    }

}
