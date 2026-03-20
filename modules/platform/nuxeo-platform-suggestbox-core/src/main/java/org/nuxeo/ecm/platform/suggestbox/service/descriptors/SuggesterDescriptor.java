/*
 * (C) Copyright 2010-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.nuxeo.runtime.model.XContextValues.CONTRIBUTING_COMPONENT;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XContext;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.Descriptor;

/**
 * XMap descriptor for registering overridable parameterized Suggester implementation on the SuggesterService.
 *
 * @author ogrisel
 */
@XObject("suggester")
public class SuggesterDescriptor implements Descriptor {

    @XNode("@name")
    protected String name = "default";

    @XNode("@class")
    protected String className;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters = new HashMap<>();

    @XContext(CONTRIBUTING_COMPONENT)
    protected ComponentInstance contributingComponent;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public ComponentInstance getContributingComponent() {
        return contributingComponent;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (SuggesterDescriptor) o;
        var merged = new SuggesterDescriptor();
        merged.name = getIfNull(other.name, name);
        // merge className and related contributingComponent
        if (StringUtils.isNotBlank(other.className)) {
            merged.className = other.className;
            merged.contributingComponent = other.contributingComponent;
        } else {
            merged.className = className;
            merged.contributingComponent = contributingComponent;
        }
        merged.className = defaultIfEmpty(other.className, className);
        merged.enabled = other.enabled;
        // merged the parameters
        merged.parameters = new HashMap<>(parameters);
        merged.parameters.putAll(other.parameters);
        return merged;
    }

    public Suggester instantiateSuggester() {
        // try build the suggester instance as early as possible to throw errors at deployment time rather than lazily
        // at first access time by the user: fail early.
        try {
            var suggester = (Suggester) contributingComponent.getRuntimeContext()
                                                             .loadClass(className)
                                                             .getDeclaredConstructor()
                                                             .newInstance();
            suggester.initWithParameters(this);
            return suggester;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(
                    String.format("Failed to instantiate suggester: %s with class: %s", name, className), e);
        } catch (ComponentInitializationException e) {
            throw new NuxeoException(String.format("Failed to init suggester: %s with class: %s", name, className), e);
        }
    }
}
