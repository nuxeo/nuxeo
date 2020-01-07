/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.ecm.core.schema.FacetNames.COMMENTABLE;
import static org.nuxeo.ecm.core.schema.FacetNames.DOWNLOADABLE;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.core.schema.FacetNames.HAS_RELATED_TEXT;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;
import static org.nuxeo.ecm.core.schema.FacetNames.MASTER_PUBLISH_SPACE;
import static org.nuxeo.ecm.core.schema.FacetNames.NOT_FULLTEXT_INDEXABLE;
import static org.nuxeo.ecm.core.schema.FacetNames.ORDERABLE;
import static org.nuxeo.ecm.core.schema.FacetNames.PUBLISHABLE;
import static org.nuxeo.ecm.core.schema.FacetNames.PUBLISH_SPACE;
import static org.nuxeo.ecm.core.schema.FacetNames.SUPER_SPACE;
import static org.nuxeo.ecm.core.schema.FacetNames.SYSTEM_DOCUMENT;
import static org.nuxeo.ecm.core.schema.FacetNames.VERSIONABLE;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestFacet {

    @Inject
    public SchemaManager schemaManager;

    @Test
    public void shouldGetFacets() {
        var facets = List.of(FOLDERISH, VERSIONABLE, ORDERABLE, DOWNLOADABLE, SUPER_SPACE, PUBLISHABLE, PUBLISH_SPACE,
                MASTER_PUBLISH_SPACE, COMMENTABLE, HIDDEN_IN_NAVIGATION, SYSTEM_DOCUMENT, NOT_FULLTEXT_INDEXABLE,
                HAS_RELATED_TEXT);

        facets.forEach(fn -> assertNotNull(String.format("Facet %s should exist", fn), schemaManager.getFacet(fn)));
    }
}
