/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Adapter to transform any java {@link Serializable} into a qualified name resource and conversely.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface ResourceAdapter {

    String CORE_SESSION_CONTEXT_KEY = "CoreSession";

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
    Resource getResource(Serializable object, Map<String, Object> context);

    /**
     * Resolves the resource to an applicative representation, for instance a {@link DocumentModel}.
     *
     * @param context a context map (holding for instance a {@link CoreSession} instance.
     * @return the representation
     */
    Serializable getResourceRepresentation(Resource resource, Map<String, Object> context);

    /**
     * @return the class being adapted
     */
    Class<?> getKlass();

}
