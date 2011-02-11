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
