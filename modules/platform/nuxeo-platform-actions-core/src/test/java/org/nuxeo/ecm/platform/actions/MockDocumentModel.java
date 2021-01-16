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
 * $Id: MockDocumentModel.java 21693 2007-07-01 08:00:36Z sfermigier $
 */

package org.nuxeo.ecm.platform.actions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class MockDocumentModel extends DocumentModelImpl {

    private static final long serialVersionUID = 8352367935191107976L;

    private final Set<String> facets;

    public MockDocumentModel(String type, String[] facets) {
        super(type);
        this.facets = new HashSet<>();
        this.facets.addAll(Arrays.asList(facets));
    }

    @Override
    public Set<String> getFacets() {
        return facets;
    }

    @Override
    public String getId() {
        return "My Document ID";
    }

    @Override
    public boolean hasFacet(String facet) {
        return facets.contains(facet);
    }

}
