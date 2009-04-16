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
 * $Id: SearchServiceInternals.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.internals;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.search.api.backend.SearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.IndexableDocType;

/**
 * Search service internal API.
 * <p>
 * This API is <strong>not</strong> exposed publicly by the service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface SearchServiceInternals extends SearchService {

    /**
     * Returns registred search engine plugins.
     *
     * @return registred search engine plugins.
     */
    Map<String, SearchEngineBackend> getSearchEngineBackends();

    /**
     * Returns a search engine plugin given its name.
     *
     * @param name : name of the search engine plugin.
     * @return a search engine backend instance
     */
    SearchEngineBackend getSearchEngineBackendByName(String name);

    /**
     * Returns the default backend.
     * <p>
     * It will be used as a fallback when no prefered backend are specified by a
     * given resource.
     * <p>
     * Note, this is the backend responsability to register itself as default.
     * And as well, the default backend can be overriden by another contributed
     * backend. In this case, one can use the deployment order offered by Nuxeo
     * runtime to ensure priority.
     * <p>
     * Returns null if no default backend registered.
     *
     * @return a search engine backend instance
     */
    String getDefaultSearchEngineBakendName();

    /**
     * Sets the default backend given its name.
     *
     * @param backendName a search engine backend instance.
     */
    void setDefaultSearchEngineBackendName(String backendName);

    /**
     * Returns the prefered backend for an indexable resolved resource.
     *
     * @param resource an indexable resolved resource.
     * @return a backend name.
     */
    String getPreferedBackendNameFor(ResolvedResource resource);

    /**
     * Returns a map from doc type to indexable doc types.
     *
     * @return a map from doc type to indexable doc type.
     */
    Map<String, IndexableDocType> getIndexableDocTypes();

    /**
     * Returns the set of document types bearing a given facet.
     *
     * @param facet the given facet
     * @return the set of names, guaranteed to be non empty, or null
     */
    Set<String> getDocumentTypeNamesForFacet(String facet);

    /**
     * Returns the set of document types bearing one of given facets.
     *
     * @param facets the given facets, as a collection
     * @return the set of names, guaranteed to be non empty, or null
     */
    Set<String> getDocumentTypeNamesForFacet(Collection<String> facets);

    /**
     * Return the names of core document types extending the given one, which
     * is included.
     *
     * @param docType the base document type.
     * @return the names as a set or null
     */
    Set<String> getDocumentTypeNamesExtending(String docType);

    /**
     * Returns the indexing data conf for a given data name.
     * <p>
     * For instance, a data name can be <code>dc:title</code>
     *
     * @param dataName the data name.
     * @return an indexable resource data conf.
     */
    IndexableResourceDataConf getIndexableDataConfFor(String dataName);

    /**
     * Returns the indexing data conf by its name.
     * <p>
     * TODO This is a temporary helper for the current flat data model To be
     * rethought
     * <p>
     * For instance, the name can be <code>Title</code> while the data name is
     * <code>dublincore:title</code>.
     *
     * @param name the data name.
     * @return an indexable resource data conf.
     */
    IndexableResourceDataConf getIndexableDataConfByName(String name);

}
