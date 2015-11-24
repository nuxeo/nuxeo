package org.nuxeo.opensocial.container.client.gadgets.facets.api;

import java.util.List;

/**
 * @author Stéphane Fourrier
 */
public interface HasFacets {
    List<Facet> getFacets();

    void addFacet(Facet facet);
}
