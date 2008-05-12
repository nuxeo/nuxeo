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
import org.nuxeo.ecm.webengine.util.VirtualDirectory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultWebApplication implements WebApplication {

    protected WebEngine engine;
    protected String id;
    protected VirtualDirectory vdir;
    protected String errorPage;
    protected String indexPage;
    protected String defaultPage;
    protected RequestHandler requestHandler;
    protected DocumentResolver documentResolver;
    protected Map<String, String> typeBindings;
    protected PathMapper mapper;

    // object binding cache
    protected ConcurrentMap<String, ObjectDescriptor> objects;


    public DefaultWebApplication(WebEngine engine, WebApplicationDescriptor desc) throws WebException {
        this.engine = engine;
        this.id = desc.getId();
        this.defaultPage = desc.getDefaultPage();
        this.indexPage = desc.getIndexPage();
        this.errorPage = desc.getErrorPage();
        this.requestHandler = desc.getRequestHandler();
        this.documentResolver = desc.getDocumentResolver();
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
            String[] rootPaths = desc.getRoots();
            File[] roots = new File[rootPaths.length];
            for (int i=0; i<rootPaths.length; i++) {
                roots[i] = new File(engine.getRootDirectory(), rootPaths[i]);
            }
            this.vdir = new VirtualDirectory(roots);
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

    public VirtualDirectory getVirtualDirectory() {
        return vdir;
    }

    /**
     * @return the requestHandler.
     */
    public RequestHandler getRequestHandler() {
        return requestHandler;
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
        String id = typeBindings.get(type);
        if (id == null) {
            id = engine.getTypeBinding(type);
        }
        return id;
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
        if (obj != null) { // not in cache
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

}
