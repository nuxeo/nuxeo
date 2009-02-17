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
package org.nuxeo.ecm.client;

import java.net.URL;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Client {
    
    URL getBaseURL();
    
    Connection get(String url);
    
    Connection get(String url, Map<String,Object> params);
    
    Connection head(String url);
    
    Connection head(String url, Map<String,Object> params);
    
    Connection delete(String url);
    
    Connection delete(String url, Map<String,Object> params);
    
    Connection put(String url, Object content);
    
    Connection post(String url, Object content);

    Connection put(String url, Map<String,Object> params);
    
    Connection post(String url, Map<String,Object> params);

    Connection put(String url, Object content, Map<String,Object> params);
    
    Connection post(String url, Object content, Map<String,Object> params);
   
}

