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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Descriptor for the character filtering service
 * @since 9.1
 */
@XObject("filtering")
public class CharacterFilteringServiceDescriptor {

    @XNode("@enabled")
    public boolean enabled;

    @XNodeList(value = "disallowedCharacters/character", type = ArrayList.class, componentType = String.class)
    public List<String> disallowedChars;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getDisallowedChars() {
        return disallowedChars;
    }

    public void setDisallowedChars(List<String> disallowedChars) {
        this.disallowedChars = disallowedChars;
    }
}
