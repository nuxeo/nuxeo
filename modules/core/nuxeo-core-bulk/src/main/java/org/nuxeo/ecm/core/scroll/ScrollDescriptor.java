/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.core.scroll;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 11.1
 */
@XObject("scroll")
public class ScrollDescriptor implements Descriptor {

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@type")
    protected String type;

    @XNode("@name")
    protected String name;

    @XNode("@default")
    protected Boolean isDefault;

    @XNode("@class")
    protected Class<? extends Scroll> scrollClass;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> options = new HashMap<>();

    protected Map<String, String> optionsReadOnly;

    @Override
    public String getId() {
        return getType() + ":" + getName();
    }

    public Scroll newScrollInstance() {
        try {
            return scrollClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Cannot create scroll class of type " + scrollClass.getName(), e);
        }
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    public boolean isDefault() {
        return toBooleanDefaultIfNull(isDefault, false);
    }

    public Map<String, String> getOptions() {
        if (optionsReadOnly == null) {
            optionsReadOnly = Collections.unmodifiableMap(options);
        }
        return optionsReadOnly;
    }
}
