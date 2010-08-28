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

import java.util.Set;

import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.ecm.webengine.security.Guard;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ResourceType {

    String ROOT_TYPE_NAME = "*";

    void flushCache();

    Guard getGuard();

    String getName();

    boolean isDerivedFrom(String type);

    Class<? extends Resource> getResourceClass();

    <T extends Resource> T newInstance();

    ResourceType getSuperType();

    Set<String> getFacets();

    boolean hasFacet(String facet);

    /**
     * Gets a view for this type in the context of the given module.
     */
    ScriptFile getView(Module module, String name);

    boolean isEnabled(Resource ctx);

}
