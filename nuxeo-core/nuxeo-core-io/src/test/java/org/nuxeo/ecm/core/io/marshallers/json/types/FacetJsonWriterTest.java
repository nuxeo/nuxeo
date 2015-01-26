/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.types;

import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;

import javax.inject.Inject;

public class FacetJsonWriterTest extends AbstractJsonWriterTest.Local<FacetJsonWriter, CompositeType> {

    public FacetJsonWriterTest() {
        super(FacetJsonWriter.class, CompositeType.class);
    }

    @Inject
    private SchemaManager schemaManager;

    @Test
    public void testFacetWithoutSchema() throws Exception {
        CompositeType type = schemaManager.getFacet("Folderish");
        JsonAssert json = jsonAssert(type);
        json.properties(2);
        json.has("entity-type").isEquals("facet");
        json.has("name").isEquals("Folderish");
    }

    @Test
    public void testFacetWithSchema() throws Exception {
        CompositeType type = schemaManager.getFacet("HasRelatedText");
        JsonAssert json = jsonAssert(type);
        json.properties(3);
        json.has("entity-type").isEquals("facet");
        json.has("name").isEquals("HasRelatedText");
        json = json.has("schemas").length(1).has(0);
        json.has("entity-type").isEquals("schema");
        json.has("name").isEquals("relatedtext");
    }

}
