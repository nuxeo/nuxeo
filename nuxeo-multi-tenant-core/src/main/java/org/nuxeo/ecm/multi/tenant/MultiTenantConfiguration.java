/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.security.SecurityConstants;

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
    private List<String> prohibitedGroups = new ArrayList<>(Arrays.asList("members", EVERYONE));

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
