/*
 * (C) Copyright 2007-2026 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.audit.service.extension;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Objects;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Extended info descriptor
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("extendedInfo")
public class ExtendedInfoDescriptor implements Descriptor {

    /** @since 2025.16 */
    public static final String ALL_EVENTS = "@all";

    /** @since 2015.16 */
    @XNode("@event")
    protected String event = ALL_EVENTS;

    @XNode("@key")
    private String key;

    @XNode("@expression")
    private String expression;

    @XNode("@enabled")
    private Boolean enabled;

    @Override
    public String getId() {
        return "%s__%s".formatted(event, key);
    }

    /** @since 2025.16 */
    public String getEvent() {
        return event;
    }

    public String getKey() {
        return key;
    }

    /** @deprecated since 2025.16, no replacement */
    @Deprecated(since = "2025.16", forRemoval = true)
    public void setKey(String value) {
        key = value;
    }

    public String getExpression() {
        return expression;
    }

    /** @deprecated since 2025.16, no replacement */
    @Deprecated(since = "2025.16", forRemoval = true)
    public void setExpression(String value) {
        expression = value;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    /** @deprecated since 2025.0, use {@link ExtendedInfoDescriptor#isEnabled()} instead */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getEnabled() {
        return enabled;
    }

    /** @deprecated since 2025.16, no replacement */
    @Deprecated(since = "2025.16", forRemoval = true)
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ExtendedInfoDescriptor merge(Descriptor o) {
        var other = (ExtendedInfoDescriptor) o;
        var merged = new ExtendedInfoDescriptor();
        merged.event = getIfNull(other.event, event);
        merged.key = getIfNull(other.key, key);
        merged.expression = defaultIfBlank(other.expression, expression);
        merged.enabled = getIfNull(other.enabled, enabled);
        return merged;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ExtendedInfoDescriptor other)) {
            return false;
        }
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
