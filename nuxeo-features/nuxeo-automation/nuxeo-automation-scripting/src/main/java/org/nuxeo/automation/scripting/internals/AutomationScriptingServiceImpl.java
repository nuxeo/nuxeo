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
 */
package org.nuxeo.automation.scripting.internals;

import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.AUTOMATION_SCRIPTING_PRECOMPILE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CACHE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.COMPLIANT_JAVA_VERSION_CLASS_FILTER;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.DEFAULT_PRECOMPILE_STATUS;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_JAVA_VERSION;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_WARN_CACHE;
import static org.nuxeo.automation.scripting.api.AutomationScriptingConstants.NASHORN_WARN_CLASS_FILTER;
import static org.nuxeo.launcher.config.ConfigurationGenerator.checkJavaVersion;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 *
 *
 * @since TODO
 */
public class AutomationScriptingServiceImpl implements AutomationScriptingService {

    final Supplier<ScriptEngine> supplier = new Factory().supplier;

    @Override
    public Session get(CoreSession session) {
        return get(new OperationContext(session));
    }

    @Override
    public Session get(OperationContext context) {
        return new Bridge(context);
    }


    final ScriptEngine engine = supplier.get();

    class Bridge implements Session {

        final CompiledScript mapperScript = AutomationMapper.compile((Compilable) engine);

        final Compilable compilable = ((Compilable) engine);

        final Invocable invocable = ((Invocable) engine);

        final ScriptContext scriptContext = engine.getContext();

        final AutomationMapper mapper;

        final ScriptObjectMirror global;

        Bridge(OperationContext operationContext) {
            mapper = new AutomationMapper(operationContext);
            try {
                mapperScript.eval(mapper);
            } catch (ScriptException cause) {
                throw new NuxeoException("Cannot execute mapper " + mapperScript, cause);
            }
            global = (ScriptObjectMirror) mapper.get("nashorn.global");
            scriptContext.setBindings(mapper, ScriptContext.ENGINE_SCOPE);
        }

        @Override
        public <T> T handleof(InputStream input, Class<T> typeof) {
            run(input);
            T handle = invocable.getInterface(global, typeof);
            if (handle == null) {
                throw new NuxeoException("Script doesn't implements " + typeof.getName());
            }
            return typeof.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class[] { typeof }, new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            return mapper.unwrap(method.invoke(handle, mapper.wrap(args[0]), mapper.wrap(args[1])));
                        }
                    }));
        }

        @Override
        public Object run(InputStream input) {
            try {
                return mapper.unwrap(engine.eval(new InputStreamReader(input), mapper));
            } catch (ScriptException cause) {
                throw new NuxeoException("Cannot evaluate automation script", cause);
            }
        }

        <T> T handleof(Class<T> typeof) {
            return invocable.getInterface(global, typeof);
        }

        @Override
        public <T> T adapt(Class<T> typeof) {
            if (typeof.isAssignableFrom(engine.getClass())) {
                return typeof.cast(engine);
            }
            if (typeof.isAssignableFrom(AutomationMapper.class)) {
                return typeof.cast(mapper);
            }
            if (typeof.isAssignableFrom(scriptContext.getClass())) {
                return typeof.cast(scriptContext);
            }
            throw new IllegalArgumentException("Cannot adapt scripting context to " + typeof.getName());
        }

        @Override
        public void close() throws Exception {
            mapper.flush();
        }
    }

    class Factory {

        final NashornScriptEngineFactory nashorn = new NashornScriptEngineFactory();

        final Supplier<ScriptEngine> supplier = supplier();

        Supplier<ScriptEngine> supplier() {

            Log log = LogFactory.getLog(AutomationScriptingServiceImpl.class);
            String version = Framework.getProperty("java.version");
            // Check if jdk8
            if (!checkJavaVersion(version, NASHORN_JAVA_VERSION)) {
                throw new UnsupportedOperationException(NASHORN_JAVA_VERSION);
            }
            // Check if version < jdk8u25 -> no cache.
            if (!checkJavaVersion(version, COMPLIANT_JAVA_VERSION_CACHE)) {
                log.warn(NASHORN_WARN_CACHE);
                return noCache();
            }
            // Check if jdk8u25 <= version < jdk8u40 -> only cache.
            if (!checkJavaVersion(version, COMPLIANT_JAVA_VERSION_CLASS_FILTER)) {
                if (Boolean.parseBoolean(
                        Framework.getProperty(AUTOMATION_SCRIPTING_PRECOMPILE, DEFAULT_PRECOMPILE_STATUS))) {
                    log.warn(NASHORN_WARN_CLASS_FILTER);
                    return cache();
                } else {
                    log.warn(NASHORN_WARN_CLASS_FILTER);
                    return noCache();
                }
            }
            // Else if version >= jdk8u40 -> cache + class filter
            try {
                if (Boolean.parseBoolean(
                        Framework.getProperty(AUTOMATION_SCRIPTING_PRECOMPILE, DEFAULT_PRECOMPILE_STATUS))) {
                    return cacheAndClassFilter();
                } else {
                    return noCacheAndClassFilter();
                }
            } catch (NoClassDefFoundError cause) {
                log.warn(NASHORN_WARN_CLASS_FILTER);
                return cache();
            }
        }

        Supplier<ScriptEngine> noCache() {
            return () -> nashorn.getScriptEngine(new String[] { "-strict" });
        }

        Supplier<ScriptEngine> cache() {
            return () -> nashorn.getScriptEngine(
                    new String[] { "-strict", "--optimistic-types=true", "--persistent-code-cache", "--class-cache-size=50" },
                    Thread.currentThread().getContextClassLoader());
        }

        Supplier<ScriptEngine> cacheAndClassFilter() {
            return () -> nashorn.getScriptEngine(
                    new String[] { "-strict", "--optimistic-types=true", "--persistent-code-cache", "--class-cache-size=50" },
                    Thread.currentThread().getContextClassLoader(), new ClassFilter() {

                        @Override
                        public boolean exposeToScripts(String className) {
                            return false;
                        }

                    });
        }

        Supplier<ScriptEngine> noCacheAndClassFilter() {
            return () -> nashorn.getScriptEngine(new String[] { "-strict" },
                    Thread.currentThread().getContextClassLoader(), new ClassFilter() {

                        @Override
                        public boolean exposeToScripts(String className) {
                            return false;
                        }

                    });
        };
    }

}
