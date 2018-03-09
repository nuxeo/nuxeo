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
import java.util.Collections;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.runtime.api.Framework;

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
        TrashService trashService = Framework.getService(TrashService.class);
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            TrashInfo info = trashService.getTrashInfo(Collections.singletonList(document),
                    wrapper.getSession().getPrincipal(), false, false);
            DocumentModel above = trashService.getAboveDocument(document, info.rootPaths);
            if (above != null) {
                writeEntityField(NAME, above, jg);
            }
        }
    }

}
