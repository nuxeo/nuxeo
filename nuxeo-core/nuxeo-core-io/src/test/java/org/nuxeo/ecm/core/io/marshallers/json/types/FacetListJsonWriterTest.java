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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;

@LocalDeploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class FacetListJsonWriterTest extends AbstractJsonWriterTest.Local<FacetListJsonWriter, List<CompositeType>> {

    public FacetListJsonWriterTest() {
        super(FacetListJsonWriter.class, List.class, TypeUtils.parameterize(List.class, CompositeType.class));
    }

    @Inject
    private SchemaManager schemaManager;

    public List<CompositeType> getElements() {
        return Arrays.asList(schemaManager.getFacet("Folderish"), schemaManager.getFacet("Publishable"));
    }

    @Test
    public void test() throws Exception {
        List<CompositeType> elements = getElements();
        JsonAssert json = jsonAssert(elements);
        json.isObject();
        json.properties(2);
        json.has("entity-type").isEquals("facets");
        json = json.has("entries").length(elements.size());
        json.childrenContains("entity-type", "facet", "facet");
        json.childrenContains("name", "Folderish", "Publishable");
    }

}
