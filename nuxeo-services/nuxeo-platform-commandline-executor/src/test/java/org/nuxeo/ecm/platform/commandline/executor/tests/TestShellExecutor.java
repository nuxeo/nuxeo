/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */

package org.nuxeo.ecm.platform.commandline.executor.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;

public class TestShellExecutor {

    private static Map<String, String> originalEnv = new HashMap<>();

    protected static void setEnv(Map<String, String> newenv) {
        // register original env
        for (String envKey : newenv.keySet()) {
            if (!originalEnv.containsKey(envKey)) {
                originalEnv.put(envKey, System.getenv(envKey));
            }
        }

        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField(
                    "theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                fail("Unable to modify environment variables");
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            fail("Unable to modify environment variables");
        }
    }

    @After
    public void resetOriginalEnv() {
        setEnv(originalEnv);
    }

    @Test
    public void testGetCommandAbsolutePath() {
        String testResourcesDirAbsPath = new File("./src/test/resources").getAbsolutePath();

        // set invalid path in the PATH environment variable (space at the end)
        Map<String, String> newenv = new HashMap<>();
        newenv.put("PATH", testResourcesDirAbsPath + " ");
        setEnv(newenv);

        String cmdAbsPath = ShellExecutor.getCommandAbsolutePath("fakecmd");
        // path must have been trimmed
        assertEquals(testResourcesDirAbsPath + File.separator + "fakecmd", cmdAbsPath);

        // set invalid path (windows) in the PATH environment variable (< at the end)
        newenv.put("PATH", "badpath<");
        setEnv(newenv);

        cmdAbsPath = ShellExecutor.getCommandAbsolutePath("fakecmd");
        // path must have been ignored, the command is returned unchanged
        assertEquals("fakecmd", cmdAbsPath);
    }
}
