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

    @XNode("queryModel")
    protected String queryModelName;

    @XNode("orderableQueryModel")
    protected String orderableQueryModelName;

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

    public String getQueryModelName() {
        return queryModelName;
    }

    public String getOrderableQueryModelName() {
        return orderableQueryModelName;
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

}
