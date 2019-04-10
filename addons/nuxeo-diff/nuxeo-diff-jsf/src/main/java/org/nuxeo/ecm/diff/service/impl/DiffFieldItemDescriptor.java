/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffFieldItemDefinitionImpl;

/**
 * Diff field item descriptor.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
@XObject("item")
public class DiffFieldItemDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@displayContentDiffLinks")
    public boolean displayContentDiffLinks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisplayContentDiffLinks() {
        return displayContentDiffLinks;
    }

    public void setDisplayContentDiffLinks(boolean displayContentDiffLinks) {
        this.displayContentDiffLinks = displayContentDiffLinks;
    }

    public DiffFieldItemDefinition getDiffFieldItemDefinition() {
        return new DiffFieldItemDefinitionImpl(getName(), isDisplayContentDiffLinks());
    }
}
