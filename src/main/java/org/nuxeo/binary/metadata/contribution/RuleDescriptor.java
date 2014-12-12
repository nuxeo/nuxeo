/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.contribution;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.1
 */
@XObject("mapping")
public class RuleDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@order")
    protected String order;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@async")
    protected Boolean isAsync;

    @XNodeList(value = "metadataMappings/metadataMapping-id", componentType = String.class, type = ArrayList.class)
    protected List<String> metadataMappingIdDescriptors;

    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    public String getId() {
        return id;
    }

    public String getOrder() {
        return order;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getIsAsync() {
        return isAsync;
    }

    public List<String> getMetadataMappingIdDescriptors() {
        return metadataMappingIdDescriptors;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }
}
