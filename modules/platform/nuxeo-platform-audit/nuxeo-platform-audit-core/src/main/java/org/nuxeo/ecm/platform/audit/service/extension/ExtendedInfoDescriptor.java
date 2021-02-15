/*
 * (C) Copyright 2007-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.audit.service.extension;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * Extended info descriptor
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("extendedInfo")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class ExtendedInfoDescriptor {

    @XNode("@key")
    @XRegistryId
    private String key;

    @XNode("@expression")
    private String expression;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    private boolean enabled;

    public String getKey() {
        return key;
    }

    public void setKey(String value) {
        key = value;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String value) {
        expression = value;
    }

    public boolean getEnabled() {
        return enabled;
    }

    @Override
    public int hashCode() {
        return key == null ? 0 : key.hashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
