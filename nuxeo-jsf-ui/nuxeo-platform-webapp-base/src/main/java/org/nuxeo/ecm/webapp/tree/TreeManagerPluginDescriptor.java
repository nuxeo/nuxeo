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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Plugin holding filter and sort configuration information for a document tree.
 *
 * @author Florent BONNET
 * @author Anahide Tchertchian
 */
@XObject("treeManagerPlugin")
public class TreeManagerPluginDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("filterClass")
    protected String filterClassName;

    @XNode("leafFilterClass")
    protected String leafFilterClassName;

    @XNode("sorterClass")
    protected String sorterClassName;

    /**
     * @since 5.4.2
     */
    @XNode("pageProvider")
    protected String pageProvider;

    @XNodeList(value = "excludedTypes/type", type = ArrayList.class, componentType = String.class)
    protected List<String> excludedTypes;

    @XNodeList(value = "excludedFacets/facet@name", type = ArrayList.class, componentType = String.class)
    protected List<String> excludedFacets;

    @XNodeList(value = "includedFacets/facet@name", type = ArrayList.class, componentType = String.class)
    protected List<String> includedFacets;

    @XNode("sortPropertyPath")
    protected String sortPropertyPath;

    public String getName() {
        return name;
    }

    public String getFilterClassName() {
        return filterClassName;
    }

    public String getLeafFilterClassName() {
        return leafFilterClassName;
    }

    public String getSorterClassName() {
        return sorterClassName;
    }

    public List<String> getExcludedTypes() {
        return excludedTypes;
    }

    public List<String> getExcludedFacets() {
        return excludedFacets;
    }

    public List<String> getIncludedFacets() {
        return includedFacets;
    }

    public String getSortPropertyPath() {
        return sortPropertyPath;
    }

    public String getPageProvider() {
        return pageProvider;
    }
}
