/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.gadgets;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.HasFacets;

import com.google.gwt.user.client.ui.Composite;

/**
 * @author Stéphane Fourrier
 */
public abstract class AbstractGadget extends Composite implements HasFacets {
    private List<Facet> facetsList = new ArrayList<Facet>();

    public List<Facet> getFacets() {
        return facetsList;
    }

    public void addFacet(Facet facet) {
        facetsList.add(facet);
    }
}
