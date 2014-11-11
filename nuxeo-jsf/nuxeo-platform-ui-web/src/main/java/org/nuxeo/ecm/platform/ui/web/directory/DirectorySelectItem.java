/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;

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
            this.setLabel("");
        }

        try {
            this.ordering = ordering;
        } catch (NumberFormatException nfe) {
            this.ordering = 0;
        }
    }

    public DirectorySelectItem(Object value, String label, long ordering,
            boolean disabled, boolean escape) {
        this(value, label, ordering);
        setDisabled(disabled);
        setEscape(escape);
    }

    /**
     * Gets the label as it should be displayed.
     *
     * @deprecated as of 5.9.6, use {@link #getLabel()} instead.
     */
    @Deprecated
    public String getDisplayedLabel() {
        return displayedLabel;
    }

    /**
     * @deprecated as of 5.9.6, use {@link #setLabel(String)} instead.
     */
    @Deprecated
    public void setDisplayedLabel(String displayedLabel) {
        this.displayedLabel = displayedLabel;
    }

    /**
     * Gets the label as it should be displayed.
     *
     * @deprecated as of 5.9.6, use {@link #getLabel()} instead.
     */
    @Deprecated
    public String getLocalizedLabel() {
        return localizedLabel;
    }

    /**
     * @deprecated as of 5.9.6, use {@link #setLabel(String)} instead.
     */
    @Deprecated
    public void setLocalizedLabel(String localizedLabel) {
        this.localizedLabel = localizedLabel;
    }

    public long getOrdering() {
        return ordering;
    }

    /**
     * @deprecated since 5.9.6, seems useless
     */
    @Deprecated
    public String getSortLabel() {
        return StringUtils.isBlank(localizedLabel) ? displayedLabel
                : localizedLabel;
    }

}