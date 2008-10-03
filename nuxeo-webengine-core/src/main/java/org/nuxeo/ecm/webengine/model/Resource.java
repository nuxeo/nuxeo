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

package org.nuxeo.ecm.webengine.model;

import java.util.Map;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.model.Adaptable;

/**
 * This interface must be implemented by any object that is dispatched on a web request through web engine.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Resource extends Adaptable {

    Resource initialize(WebContext ctx, ResourceType<?> type, Object ...  args) throws WebException;

    void dispose();
    
    WebContext getContext();    
    
    Profile getApplication();
    
    ResourceType<?> getType();
    
    String getPath();

    Resource getPrevious();
    
    Resource getNext();
    
    boolean isAction();
    
    boolean isObject();
    
    WebView getView() throws WebException;
    
    WebView getView(Map<String,Object> args) throws WebException;
    
}
