/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;
import org.nuxeo.ecm.platform.suggestbox.service.Suggester;

/**
 * XMap descriptor for registering overridable parameterized Suggester implementation on the SuggesterService.
 *
 * @author ogrisel
 */
@XObject("suggester")
@XRegistry(enable = false)
public class SuggesterDescriptor {

    @XNode(value = "@name", defaultAssignment = "default")
    @XRegistryId
    protected String name;

    @XNode("@class")
    protected Class<? extends Suggester> suggesterClass;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled")
    @XEnable
    protected boolean enabled;

    @XNodeMap(value = "parameters/parameter", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> parameters;

    protected Suggester suggester;

    public String getName() {
        return name;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public Suggester getSuggester() throws ComponentInitializationException {
        try {
            suggester = suggesterClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ComponentInitializationException(
                    String.format("Failed to initialize suggester '%s' with class '%s'", name, suggesterClass), e);
        }
        suggester.initWithParameters(this);

        return suggester;
    }

}
