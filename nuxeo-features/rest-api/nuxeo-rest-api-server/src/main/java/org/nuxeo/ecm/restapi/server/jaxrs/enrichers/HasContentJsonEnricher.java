/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.restapi.server.jaxrs.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enricher that tells whether a "Folderish" or a "Collection" has children or members.
 * 
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class HasContentJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "hasContent";

    public static final String FETCH_CHILD_QUERY = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isTrashed = 0 AND ecm:parentId = %s";

    public static final String FETCH_MEMBERS_QUERY = "SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' AND ecm:isTrashed = 0 AND collectionMember:collectionIds/* = %s";

    public HasContentJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        if (document.isFolder()) {
            jg.writeBooleanField(NAME, hasContent(document, FETCH_CHILD_QUERY));
        } else if (Framework.getService(CollectionManager.class).isCollection(document)) {
            jg.writeBooleanField(NAME, hasContent(document, FETCH_MEMBERS_QUERY));
        } else {
            jg.writeBooleanField(NAME, false);
        }
    }

    protected boolean hasContent(DocumentModel document, String query) throws IOException {
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            // Limit result set to 1 as we just want to know if there's at least one child
            return wrapper.getSession()
                          .queryProjection(String.format(query, NXQL.escapeString(document.getId())), 1, 0)
                          .size() > 0;
        }
    }

}
