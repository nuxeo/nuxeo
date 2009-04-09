/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ResourceAdapter.java 22854 2007-07-22 21:10:27Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Adapter to transform any java {@link Serializable} into a qualified name
 * resource and conversely.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface ResourceAdapter {

    String CORE_SESSION_ID_CONTEXT_KEY = "CoreSession";

    String getNamespace();

    void setNamespace(String namespace);

    /**
     * Transforms an incoming object into a Resource.
     *
     * @since 5.2-M1
     * @param object the object to transform
     * @param context a context map
     * @return the resource
     */
    Resource getResource(Serializable object, Map<String, Serializable> context);

    /**
     * Resolves the resource to an applicative representation, for instance a
     * {@link DocumentModel}.
     *
     * @param resource
     * @param context a context map (holding for instance a {@link CoreSession}
     *            instance.
     * @return the representation
     */
    Serializable getResourceRepresentation(Resource resource,
            Map<String, Serializable> context);

    /**
     * @return the class being adapted
     */
    Class<?> getKlass();

}
