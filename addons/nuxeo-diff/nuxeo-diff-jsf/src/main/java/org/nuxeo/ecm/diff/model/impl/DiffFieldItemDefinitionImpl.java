/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public DiffFieldItemDefinitionImpl(String name,
            boolean displayContentDiffLinks) {
        this.name = name;
        this.displayContentDiffLinks = displayContentDiffLinks;
    }

    public String getName() {
        return name;
    }

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
        return name + " (displayContentDiffLinks: " + displayContentDiffLinks
                + ")";
    }
}
