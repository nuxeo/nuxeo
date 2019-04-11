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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;

/**
 * Default implementation of a {@link DiffFieldItemDefinition}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffFieldItemDefinitionImpl implements DiffFieldItemDefinition {

    private static final long serialVersionUID = 1054205948632276597L;

    protected String name;

    protected boolean displayContentDiffLinks;

    public DiffFieldItemDefinitionImpl(String name) {
        this(name, false);
    }

    public DiffFieldItemDefinitionImpl(String name, boolean displayContentDiffLinks) {
        this.name = name;
        this.displayContentDiffLinks = displayContentDiffLinks;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isDisplayContentDiffLinks() {
        return displayContentDiffLinks;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof DiffFieldItemDefinition)) {
            return false;
        }

        String otherName = ((DiffFieldItemDefinition) other).getName();
        boolean otherDisplayContentDiffLinks = ((DiffFieldItemDefinition) other).isDisplayContentDiffLinks();
        if (name == null && otherName == null) {
            return true;
        }
        if (name == null || otherName == null || !name.equals(otherName)
                || displayContentDiffLinks != otherDisplayContentDiffLinks) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return name + " (displayContentDiffLinks: " + displayContentDiffLinks + ")";
    }
}
