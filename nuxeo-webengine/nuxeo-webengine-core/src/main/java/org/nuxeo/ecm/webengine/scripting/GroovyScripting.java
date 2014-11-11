/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.scripting;

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
 * For Groovy we are not using the javax.script API because we need more control over debug mode and
 * script class loader.
 * Groovy scritps will be processed by this class
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
        cache = new ConcurrentHashMap<File, Entry>();
    }

    public GroovyScripting(ClassLoader parent, CompilerConfiguration cfg) {
        loader = new GroovyClassLoader(parent, cfg);
        cache = new ConcurrentHashMap<File, Entry>();
    }

    public void addClasspath(String cp) {
        loader.addClasspath(cp);
    }

    public void addClasspathUrl(URL cp) {
        loader.addURL(cp);
    }

    public void clearCache() {
        cache = new ConcurrentHashMap<File, Entry>();
        loader.clearCache();
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return loader.loadClass(className);
    }

    public GroovyClassLoader getGroovyClassLoader() {
        return loader;
    }

    //TODO add debug mode :  return new GroovyShell(new Binding(args)).evaluate(script.getFile()); ?
    public Object eval(File file, Map<String,Object> context) throws IOException {
        return eval(file, context == null ? new Binding() : new Binding(context));
    }

    public Object eval(File file, Binding context) throws IOException {
        // convenience out global variable (for compatibility with scripts on webengine 1.0 beta)
        context.setVariable("out", System.out);
        return getScript(file, context).run();
    }

    public Class<?> compile(File file) throws IOException {
        GroovyCodeSource codeSource = new GroovyCodeSource(file);
         // do not use cache - we are maintaining our proper cache -  based on lastModified
        return loader.parseClass(codeSource, false);
    }

    public Script getScript(File file, Binding context) throws IOException {
        Class<?> klass = null;
        long lastModified = file.lastModified();
        Entry entry = cache.get(file);
        if (entry != null && entry.lastModified == lastModified) { // in cache - use it
            klass = entry.klass;
        } else { // not in cache or invalid
            klass = compile(file); // compile
            cache.put(file, new Entry(klass, lastModified)); // cache it
        }
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
