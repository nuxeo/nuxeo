/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.filter;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;

/**
 * Descriptor for the character filtering service
 *
 * @since 9.1
 */
@XObject("filtering")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class CharacterFilteringServiceDescriptor {

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    public boolean enabled;

    @XNodeList(value = "disallowedCharacters/character", type = ArrayList.class, componentType = String.class)
    public List<String> disallowedChars;

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getDisallowedChars() {
        return disallowedChars;
    }

}
