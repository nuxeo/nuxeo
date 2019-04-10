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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

import org.nuxeo.ecm.diff.model.ContentDiffDisplay;
import org.nuxeo.ecm.diff.model.DifferenceType;

/**
 * Default implementation of {@link ContentDiffDisplay}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class ContentDiffDisplayImpl extends PropertyDiffDisplayImpl implements ContentDiffDisplay {

    private static final long serialVersionUID = -3187677365094933738L;

    protected boolean displayHtmlConversion;

    protected boolean displayTextConversion;

    public ContentDiffDisplayImpl(Serializable value) {
        this(value, false, false);
    }

    public ContentDiffDisplayImpl(Serializable value, boolean displayHtmlConversion, boolean displayTextConversion) {
        super(value);
        this.displayHtmlConversion = displayHtmlConversion;
        this.displayTextConversion = displayTextConversion;
    }

    public ContentDiffDisplayImpl(Serializable value, DifferenceType differenceType) {
        this(value, differenceType, false, false);
    }

    public ContentDiffDisplayImpl(Serializable value, DifferenceType differenceType, boolean displayHtmlConversion,
            boolean displayTextConversion) {
        super(value, differenceType);
        this.displayHtmlConversion = displayHtmlConversion;
        this.displayTextConversion = displayTextConversion;
    }

    public boolean isDisplayHtmlConversion() {
        return displayHtmlConversion;
    }

    public void setDisplayHtmlConversion(boolean displayHtmlConversion) {
        this.displayHtmlConversion = displayHtmlConversion;
    }

    public boolean isDisplayTextConversion() {
        return displayTextConversion;
    }

    public void setDisplayTextConversion(boolean displayTextConversion) {
        this.displayTextConversion = displayTextConversion;
    }

    @Override
    public boolean equals(Object other) {

        if (!super.equals(other)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof ContentDiffDisplay)) {
            return false;
        }

        boolean otherDisplayHtmlConversion = ((ContentDiffDisplay) other).isDisplayHtmlConversion();
        boolean otherDisplayTextConversion = ((ContentDiffDisplay) other).isDisplayTextConversion();
        return displayHtmlConversion == otherDisplayHtmlConversion
                && displayTextConversion == otherDisplayTextConversion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" / ");
        sb.append(displayHtmlConversion);
        sb.append(" / ");
        sb.append(displayTextConversion);
        return sb.toString();
    }
}
