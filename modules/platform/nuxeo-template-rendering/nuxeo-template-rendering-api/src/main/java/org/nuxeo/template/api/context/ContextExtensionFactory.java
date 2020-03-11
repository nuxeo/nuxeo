/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.api.context;

import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Factory used to register new Context Extensions The resturned Object will be directly accessible inside the Rendering
 * context
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface ContextExtensionFactory {

    /**
     * Called before redering to let you add objects inside the rendering context.
     * <p>
     * If the method returns an object, it will be automatically pushed inside the context using the name defined in the
     * contribution.
     * </p>
     * <p>
     * you can also directly push in the provided ctx map
     * </p>
     *
     * @param currentDocument
     * @param wrapper
     * @param ctx
     * @return
     */
    Object getExtension(DocumentModel currentDocument, DocumentWrapper wrapper, Map<String, Object> ctx);
}
