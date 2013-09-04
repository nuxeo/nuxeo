/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.contributor;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.actions.ActionFilter;
import org.nuxeo.ecm.platform.actions.DefaultActionFilter;


/**
 *
 *
 * @since 5.7.3
 */
@XObject("contributor")
public class ContributorDescriptor {

    @XNode("@name")
    public String name;

    @XNodeList(value = "category", type = ArrayList.class, componentType = String.class)
    List<String> categories;


    @XNode("@class")
    public Class<? extends RestContributor> klass;

    @XNodeList(value = "filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    @XNodeList(value = "filter", type = ActionFilter[].class, componentType = DefaultActionFilter.class)
    protected ActionFilter[] filters;



    /**
     * @return
     *
     */
    public RestContributor getRestContributor() {
        try {
            return klass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    }



}
