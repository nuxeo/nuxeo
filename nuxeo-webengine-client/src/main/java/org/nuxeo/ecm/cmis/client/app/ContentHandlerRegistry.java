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


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("unchecked")
public class ContentHandlerRegistry {

//    protected Map<String, ContentHandler<?>> handlersByType;
    protected ClassMap<ContentHandler<?>> handlersByClass;
    

    public ContentHandlerRegistry() {
        handlersByClass = new ClassMap<ContentHandler<?>>();
    }
    
    public synchronized <T> ContentHandler<T>  getHandler(Class<T> clazz) {
        return (ContentHandler<T>)handlersByClass.find(clazz);
    }
        
    public synchronized void registerHandler(ContentHandler<?> handler) {
        handlersByClass.put(handler.getObjectClass(), handler);
    }

    public synchronized void unregisterHandler(Class<?> clazz) {
        handlersByClass.remove(clazz);
    }
    

//    public <T> ContentHandler<T>  getHandler(String ctype) {
//        return (ContentHandler<T>)handlersByType.get(ctype);
//    }

//    public void unregisterHandler(String ctype) {
//        handlersByType.remove(ctype);
//    }

}
