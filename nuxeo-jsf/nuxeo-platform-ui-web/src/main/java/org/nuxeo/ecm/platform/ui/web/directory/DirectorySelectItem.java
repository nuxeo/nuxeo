/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class DirectorySelectItem extends SelectItem {

    private static final long serialVersionUID = 1L;

    private String localizedLabel;

    private String displayedLabel;

    private long ordering;

    public DirectorySelectItem(Object value, String label) {
        this(value, label, 0);
    }

    public DirectorySelectItem(Object value, String label, long ordering) {
        super(value, label);
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (label == null) {
            setLabel("");
        }

        try {
            this.ordering = ordering;
        } catch (NumberFormatException nfe) {
            this.ordering = 0;
        }
    }

    public DirectorySelectItem(Object value, String label, long ordering, boolean disabled, boolean escape) {
        this(value, label, ordering);
        setDisabled(disabled);
        setEscape(escape);
    }

    /**
     * Gets the label as it should be displayed.
     *
     * @deprecated as of 6.0, use {@link #getLabel()} instead.
     */
    @Deprecated
    public String getDisplayedLabel() {
        return displayedLabel;
    }

    /**
     * @deprecated as of 6.0, use {@link #setLabel(String)} instead.
     */
    @Deprecated
    public void setDisplayedLabel(String displayedLabel) {
        this.displayedLabel = displayedLabel;
    }

    /**
     * Gets the label as it should be displayed.
     *
     * @deprecated as of 6.0, use {@link #getLabel()} instead.
     */
    @Deprecated
    public String getLocalizedLabel() {
        return localizedLabel;
    }

    /**
     * @deprecated as of 6.0, use {@link #setLabel(String)} instead.
     */
    @Deprecated
    public void setLocalizedLabel(String localizedLabel) {
        this.localizedLabel = localizedLabel;
    }

    public long getOrdering() {
        return ordering;
    }

    /**
     * @deprecated since 6.0, seems useless
     */
    @Deprecated
    public String getSortLabel() {
        return StringUtils.isBlank(localizedLabel) ? displayedLabel : localizedLabel;
    }

}
