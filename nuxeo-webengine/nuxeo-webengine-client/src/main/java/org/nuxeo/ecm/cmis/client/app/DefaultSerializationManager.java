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
 */
package org.nuxeo.ecm.cmis.client.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.nuxeo.ecm.cmis.ContentManager;
import org.nuxeo.ecm.cmis.ContentManagerException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public class DefaultSerializationManager implements SerializationManager {

    protected ClassMap<SerializationHandler<?>> handlersByClass;
    
    protected APPContentManager contentManager;

    public DefaultSerializationManager(APPContentManager cm) {
        handlersByClass = new ClassMap<SerializationHandler<?>>();
        contentManager = cm;
    }
    
    
    
    public synchronized <T> SerializationHandler<T>  getHandler(Class<T> clazz) {
        return (SerializationHandler<T>)handlersByClass.find(clazz);
    }
    
    public <T> SerializationHandler<T> getHandler(String contentType) {
        // TODO not yet implemented
        //return null;
        throw new UnsupportedOperationException("Not Yet Implemented");
    }
        
    public synchronized void registerHandler(SerializationHandler<?> handler) {
        handlersByClass.put(handler.getObjectType(), handler);
    }

    public synchronized void unregisterHandler(Class<?> clazz) {
        handlersByClass.remove(clazz);
    }    
    
    public void writeContent(Object object, OutputStream out) throws ContentManagerException {
        SerializationHandler ch = getHandler(object.getClass());
        if (ch == null) {
            throw new ContentManagerException("Content object not supported: "+object.getClass());
        }
        try {
            ch.write(object, out);
        } catch (IOException e) {
            throw new ContentManagerException("Failed to write object: "+object.getClass(), e);
        }
    }
    
    public <T> T readContent(Class<T> type, InputStream in) throws ContentManagerException {
        SerializationHandler ch = getHandler(type);
        if (ch == null) {
            throw new ContentManagerException("Content object not supported: "+type);
        }
        try {
            return (T)ch.read(in);
        } catch (IOException e) {
            throw new ContentManagerException("Failed to read object: "+type, e);
        }
    }


    public ContentManager getContentManager() {
           return contentManager;
    }
    
}
