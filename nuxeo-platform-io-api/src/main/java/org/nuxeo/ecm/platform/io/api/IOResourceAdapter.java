/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
     * @param sources locations of documents to consider. Has to include
     *            documents children if needed.
     *
     * @return a structure holding associated resources.
     */
    IOResources extractResources(String repo, Collection<DocumentRef> sources);

    /**
     * Returns translated resources once copy has been done, passing a
     * correspondence map.
     *
     * @param repo target repository for resources.
     * @param resources resources previously extracted thanks to
     *            {@link IOResourceAdapter#extractResources(String, Collection)}
     * @param map correspondence map between old locations and new ones.
     *
     * @return translated resources.
     */
    IOResources translateResources(String repo, IOResources resources,
            DocumentTranslationMap map);

    /**
     * Persists resources.
     *
     * @param newResources resources previously extracted thanks to
     *            {@link IOResourceAdapter#extractResources(String, Collection)}
     *            or
     *            {@link IOResourceAdapter#translateResources(String, IOResources, DocumentTranslationMap)}
     */
    void storeResources(IOResources newResources);

    /**
     * Export resources as XML.
     *
     * @param out: stream where export will be written.
     * @param newResources resources previously extracted thanks to
     *            {@link IOResourceAdapter#extractResources(String, Collection)}
     *            or
     *            {@link IOResourceAdapter#translateResources(String, IOResources, DocumentTranslationMap)}
     */
    void getResourcesAsXML(OutputStream out, IOResources newResources);

    /**
     * Returns resources built from given stream.
     */
    IOResources loadResourcesFromXML(InputStream stream);

}
