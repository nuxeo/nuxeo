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

    @Override
    public boolean isDisplayHtmlConversion() {
        return displayHtmlConversion;
    }

    public void setDisplayHtmlConversion(boolean displayHtmlConversion) {
        this.displayHtmlConversion = displayHtmlConversion;
    }

    @Override
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
