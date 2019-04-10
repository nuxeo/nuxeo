/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@XObject("configuration")
public class MultiTenantConfiguration {

    @XNode("tenantDocumentType")
    protected String tenantDocumentType;

    @XNode("membersGroupPermission")
    protected String membersGroupPermission = SecurityConstants.READ;

    @XNode("enabledByDefault")
    protected boolean enabledByDefault = false;

    @XNodeList(value = "prohibitedGroups/group", type = ArrayList.class, componentType = String.class)
    private List<String> prohibitedGroups = new ArrayList<String>(Arrays.asList("members",EVERYONE));
    
    public String getTenantDocumentType() {
        return tenantDocumentType;
    }

    public String getMembersGroupPermission() {
        return membersGroupPermission;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public List<String> getProhibitedGroups() {
        return prohibitedGroups;
    }
    
}
