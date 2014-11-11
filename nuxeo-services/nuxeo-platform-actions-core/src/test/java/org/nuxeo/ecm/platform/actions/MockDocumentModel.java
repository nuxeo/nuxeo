/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public final class MockDocumentModel extends DocumentModelImpl {

    private static final long serialVersionUID = 8352367935191107976L;

    private final Set<String> facets;

    public MockDocumentModel(String type, String[] facets) {
        super(type);
        this.facets = new HashSet<String>();
        this.facets.addAll(Arrays.asList(facets));
    }

    @Override
    public Set<String> getDeclaredFacets() {
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
