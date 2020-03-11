/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class GroovyScripting {

    protected final GroovyClassLoader loader;

    // compiled script class cache
    protected Map<File, Entry> cache;

    public static ClassLoader getParentLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl == null ? GroovyScripting.class.getClassLoader() : cl;
    }

    public GroovyScripting() {
        this(getParentLoader(), new CompilerConfiguration());
    }

    public GroovyScripting(boolean debug) {
        this(getParentLoader(), debug);
    }

    public GroovyScripting(ClassLoader parent, boolean debug) {
        CompilerConfiguration cfg = new CompilerConfiguration();
        cfg.setDebug(debug);
        if (debug) {
            cfg.setRecompileGroovySource(true);
        }
        loader = new GroovyClassLoader(parent, cfg);
        cache = new ConcurrentHashMap<>();
    }

    public GroovyScripting(ClassLoader parent, CompilerConfiguration cfg) {
        loader = new GroovyClassLoader(parent, cfg);
        cache = new ConcurrentHashMap<>();
    }

    public void addClasspath(String cp) {
        loader.addClasspath(cp);
    }

    public void addClasspathUrl(URL cp) {
        loader.addURL(cp);
    }

    public void clearCache() {
        cache = new ConcurrentHashMap<>();
        loader.clearCache();
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    public GroovyClassLoader getGroovyClassLoader() {
        return loader;
    }

    // TODO add debug mode : return new GroovyShell(new
    // Binding(args)).evaluate(script.getFile()); ?
    public Object eval(File file, Map<String, Object> context) throws IOException {
        return eval(file, context == null ? new Binding() : new Binding(context));
    }

    public Object eval(File file, Binding context) throws IOException {
        // convenience out global variable (for compatibility with scripts on
        // webengine 1.0 beta)
        context.setVariable("out", System.out);
        return getScript(file, context).run();
    }

    public Class<?> compile(File file) throws IOException {
        GroovyCodeSource codeSource = new GroovyCodeSource(file);
        // do not use cache - we are maintaining our proper cache - based on
        // lastModified
        return loader.parseClass(codeSource, false);
    }

    public Script getScript(File file, Binding context) throws IOException {
        Class<?> klass = null;
        long lastModified = file.lastModified();
        Entry entry = cache.get(file);
        if (entry != null && entry.lastModified == lastModified) { // in cache -
                                                                   // use it
            klass = entry.klass;
        } else { // not in cache or invalid
            klass = compile(file); // compile
            cache.put(file, new Entry(klass, lastModified)); // cache it
        }
        return InvokerHelper.createScript(klass, context);
    }

    public Script getScript(String content, Binding context) {
        Class<?> klass = loader.parseClass(content); // compile
        return InvokerHelper.createScript(klass, context);
    }

    static class Entry {
        final long lastModified;

        final Class<?> klass;

        Entry(Class<?> klass, long lastModified) {
            this.klass = klass;
            this.lastModified = lastModified;
        }
    }

}
