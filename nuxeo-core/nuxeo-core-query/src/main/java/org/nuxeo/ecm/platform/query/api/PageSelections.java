/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
            this.entries = new ArrayList<>(c);
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

    /**
     * @since 7.4
     */
    public boolean isEmpty() {
        return getSize() == 0;
    }
}
