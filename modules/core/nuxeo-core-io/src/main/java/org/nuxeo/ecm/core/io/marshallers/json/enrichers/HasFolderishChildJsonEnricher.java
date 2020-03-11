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

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.query.sql.NXQL;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 8.10
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class HasFolderishChildJsonEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "hasFolderishChild";

    public HasFolderishChildJsonEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel document) throws IOException {
        if (!document.isFolder()) {
            jg.writeBooleanField(NAME, false);
            return;
        }
        try (SessionWrapper wrapper = ctx.getSession(document)) {
            String fetchFolderishChildQuery = "SELECT * FROM Document WHERE ecm:mixinType = 'Folderish'"
                    + " AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isTrashed = 0"
                    + " AND ecm:parentId = " + NXQL.escapeString(document.getId());
            // Limit result set to 1 as we just want to know if there's at least one Folderish child
            boolean hasChildren = !wrapper.getSession().queryProjection(fetchFolderishChildQuery, 1, 0).isEmpty();
            jg.writeBooleanField(NAME, hasChildren);
        }
    }

}
