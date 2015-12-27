/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IOResourceAdapter.java 25080 2007-09-18 14:52:20Z atchertchian $
 */

package org.nuxeo.ecm.platform.io.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;

/**
 * Resource adapter holding the import/export for document associated resources.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface IOResourceAdapter extends Serializable {

    /**
     * Returns properties.
     */
    Map<String, Serializable> getProperties();

    /**
     * Set properties.
     */
    void setProperties(Map<String, Serializable> properties);

    /**
     * Extracts resources for given document locations.
     *
     * @param repo TODO
     * @param sources locations of documents to consider. Has to include documents children if needed.
     * @return a structure holding associated resources.
     */
    IOResources extractResources(String repo, Collection<DocumentRef> sources);

    /**
     * Returns translated resources once copy has been done, passing a correspondence map.
     *
     * @param repo target repository for resources.
     * @param resources resources previously extracted thanks to
     *            {@link IOResourceAdapter#extractResources(String, Collection)}
     * @param map correspondence map between old locations and new ones.
     * @return translated resources.
     */
    IOResources translateResources(String repo, IOResources resources, DocumentTranslationMap map);

    /**
     * Persists resources.
     *
     * @param newResources resources previously extracted thanks to
     *            {@link IOResourceAdapter#extractResources(String, Collection)} or
     *            {@link IOResourceAdapter#translateResources(String, IOResources, DocumentTranslationMap)}
     */
    void storeResources(IOResources newResources);

    /**
     * Export resources as XML.
     *
     * @param out stream where export will be written.
     * @param newResources resources previously extracted thanks to
     *            {@link IOResourceAdapter#extractResources(String, Collection)} or
     *            {@link IOResourceAdapter#translateResources(String, IOResources, DocumentTranslationMap)}
     */
    void getResourcesAsXML(OutputStream out, IOResources newResources);

    /**
     * Returns resources built from given stream.
     */
    IOResources loadResourcesFromXML(InputStream stream);

}
