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

import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.model.Adaptable;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Resource extends Adaptable {

    Resource initialize(WebContext ctx, ResourceType type, Object ...  args) throws WebException;

    void dispose();
    
    WebContext getContext();    
    
    Module getModule();
    
    ResourceType getType();
    
    boolean isInstanceOf(String type);
    
    String getPath();

    Resource getPrevious();
    
    Resource getNext();
        
    boolean isService();
    
    boolean isModule();
    
    boolean isRoot();
    public void setRoot(boolean isRoot);

    Set<String> getFacets();
    boolean hasFacet(String facet);

    public List<LinkDescriptor> getLinks(String category);

    
    Resource newObject(String type, Object ... args);
    ServiceResource newService(String type, Object ... args);
    Template newTemplate(String fileName);

    Response redirect(String uri);
    
}
