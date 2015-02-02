/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * List of {@link PageSelection} elements.
 *
 * @author Anahide Tchertchian
 */
public class PageSelections<T> {

    protected String name;

    protected boolean selected;

    protected List<PageSelection<T>> entries;

    public PageSelections() {
        super();
    }

    public PageSelections(Collection<? extends PageSelection<T>> c) {
        super();
        if (c != null) {
            this.entries = new ArrayList<PageSelection<T>>(c);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (entries != null) {
            for (PageSelection<T> item : entries) {
                item.setSelected(selected);
            }
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public List<PageSelection<T>> getEntries() {
        return entries;
    }

    public void setEntries(List<PageSelection<T>> entries) {
        this.entries = entries;
    }

    /**
     * @since 6.0
     */
    public void add(PageSelection<T> entry) {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        entries.add(entry);
    }

    public int getSize() {
        if (entries == null) {
            return 0;
        } else {
            return entries.size();
        }
    }

    /**
     * @deprecated just here for compatibility with SelectDatamodel methods, use {@link #getSize()} instead
     */
    @Deprecated
    public int getRowCount() {
        return getSize();
    }

    /**
     * @deprecated just here for compatibility with SelectDatamodel methods, use {@link #getEntries()} instead
     */
    @Deprecated
    public List<PageSelection<T>> getRows() {
        return getEntries();
    }

}
