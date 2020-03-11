/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.model.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Manage persistent components. Persistent components are located in ${nxserver_data_dir}/components directory, and can
 * be dynamically removed or registered. After framework startup (after the application was completely started) the
 * persistent components are deployed. The layout of the components directory is the following:
 *
 * <pre>
 * components/
 *     component1.xml
 *     component2.xml
 *     ...
 *     bundle_symbolicName1/
 *         component1.xml
 *         component2.xml
 *         ...
 *     bundle_symbolicName1/
 *         ...
 *     ...
 * </pre>
 *
 * If components are put directly under the root then they will be deployed in the runtime bundle context. If they are
 * put in a directory having as name the symbolicName of a bundle in the system, then the component will be deployed in
 * that bundle context.
 * <p>
 * Any files not ending with .xml are ignored. Any directory that doesn't match a bundle symbolic name will be ignored
 * too.
 * <p>
 * Dynamic components must use the following name convention: (it is not mandatory but it is recommended)
 * <ul>
 * <li>Components deployed in root directory must use as name the file name without the .xml extension.
 * <li>Components deployed in a bundle directory must use the relative file path without the .xml extensions.
 * </ul>
 * Examples: Given the following component files: <code>components/mycomp1.xml</code> and
 * <code>components/mybundle/mycomp2.xml</code> the name for <code>mycomp1</code> must be: <code>comp1</code> and for
 * <code>mycomp2</code> must be <code>mybundle/mycomp2</code>
 * <p>
 * This service is working only with {@link OSGiRuntimeService}
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentPersistence {

    protected final File root; // a directory to keep exploded extensions

    protected final RuntimeContext sysrc;

    protected final OSGiRuntimeService runtime;

    protected final ReadWriteLock fileLock;

    protected final Set<RegistrationInfo> persistedComponents;

    public ComponentPersistence(OSGiRuntimeService runtime) {
        this.runtime = runtime;
        root = new File(Environment.getDefault().getData(), "components");
        fileLock = new ReentrantReadWriteLock();
        sysrc = runtime.getContext();
        persistedComponents = Collections.synchronizedSet(new HashSet<>());
    }

    public File getRoot() {
        return root;
    }

    public final RuntimeContext getContext(String symbolicName) {
        if (symbolicName == null) {
            return sysrc;
        }
        Bundle bundle = runtime.getBundle(symbolicName);
        if (bundle == null) {
            return null;
        }
        return runtime.createContext(bundle);
    }

    protected void deploy(RuntimeContext rc, File file) throws IOException {
        RegistrationInfoImpl ri = (RegistrationInfoImpl) rc.deploy(file.toURI().toURL());
        ri.isPersistent = true;
    }

    public void loadPersistedComponents() throws IOException {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    RuntimeContext rc = getContext(file.getName());
                    if (rc != null) {
                        loadPersistedComponents(rc, file);
                    }
                } else if (file.isFile() && file.getName().endsWith(".xml")) {
                    deploy(sysrc, file);
                }
            }
        }
    }

    public void loadPersistedComponents(RuntimeContext rc, File root) throws IOException {
        File[] files = root.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".xml")) {
                    deploy(rc, file);
                }
            }
        }
    }

    public void loadPersistedComponent(File file) throws IOException {
        file = file.getCanonicalFile();
        if (file.isFile() && file.getName().endsWith(".xml")) {
            File parent = file.getParentFile();
            if (root.equals(parent)) {
                deploy(sysrc, file);
                return;
            } else {
                String symbolicName = parent.getName();
                parent = parent.getParentFile();
                if (root.equals(parent)) {
                    RuntimeContext rc = getContext(symbolicName);
                    if (rc != null) {
                        deploy(rc, file);
                        return;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Invalid component file location or bundle not found");
    }

    public Document loadXml(File file) throws IOException {
        byte[] bytes = safeReadFile(file);
        return loadXml(new ByteArrayInputStream(bytes));
    }

    public static Document loadXml(InputStream in) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void createComponent(byte[] bytes) throws IOException {
        createComponent(bytes, true);
    }

    public synchronized void createComponent(byte[] bytes, boolean isPersistent) throws IOException {
        Document doc = loadXml(new ByteArrayInputStream(bytes));
        Element root = doc.getDocumentElement();
        String name = root.getAttribute("name");
        int p = name.indexOf(':');
        if (p > -1) {
            name = name.substring(p + 1);
        }
        p = name.indexOf('/');
        String owner = null;
        if (p > -1) {
            owner = name.substring(0, p);
        }
        DefaultRuntimeContext rc = (DefaultRuntimeContext) getContext(owner);
        if (rc == null) {
            throw new IllegalArgumentException("Invalid component name: " + name);
        }
        File file = new File(this.root, name + ".xml");
        if (!isPersistent) {
            file.deleteOnExit();
        }
        file.getParentFile().mkdirs();
        safeWriteFile(bytes, file);
        rc.deploy(file.toURI().toURL());
    }

    public synchronized boolean removeComponent(String compName) throws IOException {
        String path = compName + ".xml";
        File file = new File(root, path);
        if (!file.isFile()) {
            return false;
        }
        int p = compName.indexOf('/');
        String owner = null;
        if (p > -1) {
            owner = compName.substring(0, p);
        }
        DefaultRuntimeContext rc = (DefaultRuntimeContext) getContext(owner);
        if (rc == null) {
            throw new IllegalArgumentException("Invalid component name: " + compName);
        }
        rc.undeploy(file.toURI().toURL());
        file.delete();
        return true;
    }

    protected void safeWriteFile(byte[] bytes, File file) throws IOException {
        fileLock.writeLock().lock();
        try {
            FileUtils.writeByteArrayToFile(file, bytes);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    protected byte[] safeReadFile(File file) throws IOException {
        fileLock.readLock().lock();
        try {
            return FileUtils.readFileToByteArray(file);
        } finally {
            fileLock.readLock().unlock();
        }
    }

}
