/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Gets the closest document's ancestor.
 * <p>
 * This is used to find what safe document to redirect to when deleting some.
 *
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class FirstAccessibleAncestorJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "firstAccessibleAncestor";

    public FirstAccessibleAncestorJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            final var currentSession = wrapper.getSession();
            if (!wrapper.getSession().exists(document.getRef())) {
                return;
            }
            if (document.getPath().isRoot()) {
                return;
            }
            DocumentRef aboveRef = CoreInstance.doPrivileged(currentSession, adminSession -> {
                // Traverse up the document hierarchy to find the first ancestor
                // that the current user has READ permission on
                var parentRef = document.getParentRef();
                while (adminSession.exists(parentRef)) {
                    if (adminSession.hasPermission(currentSession.getPrincipal(), parentRef, SecurityConstants.READ)) {
                        // found ancestor, possibly root itself
                        return parentRef;
                    } else if (adminSession.getDocument(parentRef).getPath().isRoot()) {
                        // no ancestor
                        return null;
                    }
                    parentRef = adminSession.getDocument(parentRef).getParentRef();
                }
                return null;
            });
            if (aboveRef != null) {
                writeEntityField(NAME, currentSession.getDocument(aboveRef), jg);
            }
        }
    }

}
