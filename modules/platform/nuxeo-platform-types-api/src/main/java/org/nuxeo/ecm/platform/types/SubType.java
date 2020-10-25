/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Type view to display a given document sub-type.
 *
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@XObject("type")
public class SubType {

    @XNode
    protected String name;

    protected List<String> hidden;

    public List<String> getHidden() {
        if (hidden == null) {
            hidden = new ArrayList<>();
        }
        return hidden;
    }

    @XNode("@hidden")
    public void setHidden(String value) {
        String[] hiddenCases = value.split("(\\s+)(?=[^,])|(\\s*,\\s*)");
        hidden = new ArrayList<>(Arrays.asList(hiddenCases));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Clone method to handle hot reload
     *
     * @since 5.6
     */
    @Override
    protected SubType clone() {
        SubType clone = new SubType();
        clone.setName(getName());
        List<String> hidden = getHidden();
        if (hidden != null) {
            List<String> chidden = new ArrayList<>();
            chidden.addAll(hidden);
            clone.hidden = chidden;
        }
        return clone;
    }
}
