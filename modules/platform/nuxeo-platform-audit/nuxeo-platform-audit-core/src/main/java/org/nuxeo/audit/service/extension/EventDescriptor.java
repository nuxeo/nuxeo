/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.nuxeo.audit.service.AuditComponent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Really simple auditable event descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @deprecated since 2025.16, use {@link AuditRouteDescriptor} instead
 */
@XObject("event")
@Deprecated(since = "2025.16", forRemoval = true)
public class EventDescriptor implements Descriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private Boolean enabled;

    @XNodeList(value = "extendedInfos/extendedInfo", type = ArrayList.class, componentType = ExtendedInfoDescriptor.class)
    protected List<ExtendedInfoDescriptor> extendedInfoDescriptors;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    /** @deprecated since 2025.0, use {@link EventDescriptor#isEnabled()} instead */
    @Deprecated(since = "2025.0", forRemoval = true)
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @since 7.4
     */
    public List<ExtendedInfoDescriptor> getExtendedInfoDescriptors() {
        return extendedInfoDescriptors;
    }

    /**
     * @since 7.4
     */
    public void setExtendedInfoDescriptors(List<ExtendedInfoDescriptor> extendedInfoDescriptors) {
        this.extendedInfoDescriptors = extendedInfoDescriptors;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (EventDescriptor) o;
        var merged = new EventDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.extendedInfoDescriptors = Stream.concat(extendedInfoDescriptors.stream(),
                other.extendedInfoDescriptors.stream())
                                               .collect(collectingAndThen(
                                                       toMap(ExtendedInfoDescriptor::getKey, Function.identity(),
                                                               ExtendedInfoDescriptor::merge),
                                                       map -> new ArrayList<>(map.values())));
        return merged;
    }

    /** @since 2025.16 */
    public AuditRouteDescriptor toAuditRoute() {
        var route = new AuditRouteDescriptor();
        route.name = "default";
        route.backendName = AuditComponent.DEFAULT_AUDIT_BACKEND;
        var routeEvent = new AuditRouteDescriptor.EventDescriptor();
        routeEvent.name = name;
        routeEvent.enabled = enabled;
        route.events = List.of(routeEvent);
        return route;
    }

    /** @since 2025.16 */
    public List<ExtendedInfoDescriptor> toExtendedInfos() {
        return extendedInfoDescriptors.stream().map(descriptor -> {
            var merged = descriptor.merge(new ExtendedInfoDescriptor());
            merged.event = name;
            return merged;
        }).toList();
    }
}
