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

import java.util.Set;

import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.security.Guard;

import com.sun.jersey.api.core.ResourceContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ResourceType {

    String ROOT_TYPE_NAME = "*";

    void flushCache();

    Guard getGuard();

    String getName();

    boolean isDerivedFrom(String type);

    Class<? extends Resource> getResourceClass();

    <T extends Resource> T newInstance(ResourceContext resources);

    ResourceType getSuperType();

    Set<String> getFacets();

    boolean hasFacet(String facet);

    /**
     * Gets a view for this type in the context of the given module.
     */
    ScriptFile getView(Module module, String name);

    boolean isEnabled(Resource ctx);

}
