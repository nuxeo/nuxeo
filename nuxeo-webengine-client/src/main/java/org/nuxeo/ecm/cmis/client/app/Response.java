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

import java.io.InputStream;

import org.nuxeo.ecm.cmis.ContentManagerException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Response {

    int getStatusCode();
    
    String getHeader(String key);
    
    InputStream getStream() throws ContentManagerException;
    
    byte[] getBytes() throws ContentManagerException;
    
    String getString() throws ContentManagerException;
    
    /**
     * Unmarshall response content given the response content type 
     * @return
     * @throws ContentManagerException
     */
    //Object getContent() throws ContentManagerException;
    
   /**
    * Unmarshall the response content given a class for the resulting object
    * @param <T>
    * @param clazz
    * @return
    * @throws ContentManagerException
    */
    <T> T getEntity(Object context, Class<T> clazz) throws ContentManagerException;
    
    /**
     * Get a feed object constructed from the response
     * @param <T>
     * @param context
     * @param clazz
     * @return
     */
    <T> Feed<T> getFeed(Object context, Class<T> clazz) throws ContentManagerException;
    
    boolean isOk();
    
}
