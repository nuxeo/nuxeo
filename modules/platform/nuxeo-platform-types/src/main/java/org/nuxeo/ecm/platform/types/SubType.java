/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Catalin Baican
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Type view to display a given document sub-type.
 */
@XObject("type")
public class SubType {

    @XNode
    protected String name;

    protected Set<String> hidden = new HashSet<>();

    // needed by xmap
    public SubType() {
    }

    /**
     * Helper for registry API.
     *
     * @since 11.5
     */
    public SubType(String name, Set<String> hidden) {
        this.name = name;
        if (hidden != null) {
            this.hidden.addAll(hidden);
        }
    }

    public List<String> getHidden() {
        if (hidden == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(hidden);
    }

    @XNode("@hidden")
    public void setHidden(String value) {
        String[] hiddenCases = value.split("(\\s+)(?=[^,])|(\\s*,\\s*)");
        hidden = new HashSet<>(Arrays.asList(hiddenCases));
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "{SubType: " + name + ", hidden: " + hidden + "}";
    }

}
