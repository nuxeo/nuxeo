/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import java.text.ParseException;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Resource extends Adaptable {

    Resource initialize(WebContext ctx, ResourceType type, Object... args);

    void dispose();

    WebContext getContext();

    Module getModule();

    ResourceType getType();

    boolean isInstanceOf(String type);

    String getName();

    String getPath();

    String getTrailingPath();

    String getNextSegment();

    String getURL();

    Resource getPrevious();

    Resource getNext();

    void setNext(Resource next);

    void setPrevious(Resource previous);

    boolean isAdapter();

    boolean isRoot();

    void setRoot(boolean isRoot);

    Set<String> getFacets();

    boolean hasFacet(String facet);

    List<LinkDescriptor> getLinks(String category);

    Resource newObject(String type, Object... args);

    AdapterResource newAdapter(String type, Object... args);

    Template getView(String viewId);

    Template getTemplate(String fileName);

    Response redirect(String uri);

    /**
     * Returns the active Adapter on this object if any in the current request.
     *
     * @return the service instance or null
     */
    AdapterResource getActiveAdapter();

    /**
     * Checks the given guard expression in the context of this resource.
     */
    boolean checkGuard(String guard) throws ParseException;

    // Response getEntries();

}
