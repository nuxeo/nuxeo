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

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.webengine.exceptions.WebDeployException;
import org.nuxeo.ecm.webengine.mapping.Mapping;
import org.nuxeo.ecm.webengine.mapping.MappingDescriptor;
import org.nuxeo.ecm.webengine.mapping.PathMapper;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.util.DirectoryStack;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebApplication implements WebApplication {

    protected WebEngine engine;
    protected String id;
    protected DirectoryStack vdir;
    protected String errorPage;
    protected String indexPage;
    protected String defaultPage;
    protected DocumentResolver documentResolver;
    protected Map<String, String> typeBindings;
    protected PathMapper mapper;

    // object binding cache
    protected ConcurrentMap<String, ObjectDescriptor> objects;


    public DefaultWebApplication(WebEngine engine, WebApplicationDescriptor desc) throws WebException {
        this.engine = engine;
        this.id = desc.getId();
        this.defaultPage = desc.getDefaultPage("default.ftl");
        this.indexPage = desc.getIndexPage("index.ftl");
        this.errorPage = desc.getErrorPage("error.ftl");
        this.documentResolver = desc.getDocumentResolver();
        if (this.documentResolver == null) {
            this.documentResolver = new DefaultDocumentResolver();
        }
        List<ObjectBindingDescriptor> list = desc.getBindings();
        if (list != null && !list.isEmpty()) {
            typeBindings = new HashMap<String, String>();
            for (ObjectBindingDescriptor obd : list) {
                typeBindings.put(obd.type, obd.objectId);
            }
        }
        List<MappingDescriptor> mappings = desc.getMappings();
        if (mappings != null && !mappings.isEmpty()) {
            mapper = new PathMapper(mappings);
        }
        try {
            RootDescriptor[] roots = desc.getRoots();
            this.vdir = new DirectoryStack();
            if (roots == null) {
                this.vdir.addDirectory(new File(engine.getRootDirectory(), "default"), 0);
            } else {
                for (int i=0; i<roots.length; i++) {
                    File file =new File(engine.getRootDirectory(), roots[i].path);
                    this.vdir.addDirectory(file, roots[i].priority);
                }
            }
        } catch (IOException e) {
            throw new WebDeployException("Failed to create virtual directory for webapp: "+id, e);
        }

        objects = new ConcurrentHashMap<String, ObjectDescriptor>();
    }


    /**
     * @return the engine.
     */
    public WebEngine getEngine() {
        return engine;
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    public DirectoryStack getVirtualDirectory() {
        return vdir;
    }

    /**
     * @return the documentResolver.
     */
    public DocumentResolver getDocumentResolver() {
        return documentResolver;
    }

    /**
     * @return the defaultPage.
     */
    public String getDefaultPage() {
        return defaultPage;
    }

    /**
     * @return the errorPage.
     */
    public String getErrorPage() {
        return errorPage;
    }

    /**
     * @return the indexPage.
     */
    public String getIndexPage() {
        return indexPage;
    }

    public Mapping getMapping(String pathInfo) {
        if (mapper == null) return null;
        return mapper.getMapping(pathInfo);
    }


    public String getTypeBinding(String type) {
        if (typeBindings != null) {
            String id = typeBindings.get(type);
            if (id != null) {
                return id;
            }
        }
        return engine.getTypeBinding(type);
    }

    public ObjectDescriptor getDefaultObject() {
        ObjectDescriptor obj = objects.get("Document");
        if (obj == null) {
            String id = getTypeBinding("Document");
            if (id == null) {
                throw new WebRuntimeException("Invalid configuration: The default object (the onbe mapped to Document type) was not declared");
            }
            obj = engine.getObject(id);
            if (obj == null) {
                throw new WebRuntimeException("Invalid configuration: The default object (the onbe mapped to Document type) was not declared");
            }
            objects.put("Document", obj);
        }
        return obj;
    }

    public synchronized ObjectDescriptor getObjectDescriptor(Type type) {
        String typeName = type.getName();
        ObjectDescriptor obj = objects.get(typeName);
        if (obj != null) { // in cache
            return obj;
        }
        String id = getTypeBinding(typeName);
        if (id == null) {
            Type stype = type.getSuperType();
            if (stype == null || stype.getName().equals("Document")) {// the default
                obj = getDefaultObject();
            } else {
                obj = getObjectDescriptor(stype);
            }
        } else {
            obj = engine.getObject(id);
            if (obj == null) {
                Type stype = type.getSuperType();
                if (stype == null || stype.getName().equals("Document")) {// the default
                    obj = getDefaultObject();
                } else {
                    obj = getObjectDescriptor(stype);
                }
            }
        }
        objects.put(typeName, obj);
        return obj;
    }

    public void flushCache() {
        objects.clear();
        vdir.flush();
    }

    public ScriptFile getScript(String path) throws IOException {
        File file = getFile(path);
        return file != null ? new ScriptFile(file) : null;
    }

    public File getFile(String path) throws IOException {
        if (!path.startsWith("/")) {
            File file = new File(path); // TODO track current executed script to be able to resolve files
            if (file.exists()) {
                return file;
            }
            path ="/"+path; // avoid doing duplicate entries in vdir cache
        }
        return vdir.getFile(path);
    }

    public WebEngine getWebEngine() {
        return engine;
    }

    public void loadConfiguration(WebApplicationDescriptor desc) throws WebDeployException {
        String val = desc.getDefaultPage();
        if (val != null) {
            this.defaultPage = val;
        }
        val = desc.getIndexPage();
        if (val != null) {
            this.indexPage = val;
        }
        val = desc.getErrorPage();
        if (val != null) {
            this.errorPage = val;
        }
        DocumentResolver dr = desc.getDocumentResolver();
        if (dr != null) {
            documentResolver = dr;
        }

        List<ObjectBindingDescriptor> list = desc.getBindings();
        if (list != null && !list.isEmpty()) {
            if (typeBindings == null) {
                typeBindings = new HashMap<String, String>();
            }
            for (ObjectBindingDescriptor obd : list) {
                typeBindings.put(obd.type, obd.objectId);
            }
            objects.clear(); // reset cache
        }

        List<MappingDescriptor> mappings = desc.getMappings();
        if (mappings != null && !mappings.isEmpty()) {
            if (mapper == null) {
                mapper = new PathMapper(mappings);
            } else {
                for (MappingDescriptor md : mappings) {
                    mapper.addMapping(md);
                }
            }
        }

        RootDescriptor[] roots = desc.getRoots();
        if (roots != null && roots.length > 0) {
        try {
            for (int i=0; i<roots.length; i++) {
                vdir.addDirectory(new File(engine.getRootDirectory(), roots[i].path), roots[i].priority);
            }
            Collections.sort(vdir.getEntries());
        } catch (IOException e) {
            throw new WebDeployException("Failed to create virtual directory for webapp: "+id, e);
        }
        }

    }

}
