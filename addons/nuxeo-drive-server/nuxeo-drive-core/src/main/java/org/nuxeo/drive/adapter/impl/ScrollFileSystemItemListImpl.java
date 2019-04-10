/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Default implementation of a {@link ScrollFileSystemItemList} based on an {@link ArrayList}.
 *
 * @since 8.3
 */
@JsonDeserialize(using = ScrollFileSystemItemListDeserializer.class)
public class ScrollFileSystemItemListImpl extends ArrayList<FileSystemItem> implements ScrollFileSystemItemList {

    private static final long serialVersionUID = 8703448908774574014L;

    protected String scrollId;

    public ScrollFileSystemItemListImpl() {
        // Needed for JSON deserialization
    }

    public ScrollFileSystemItemListImpl(String scrollId, int size) {
        super(size);
        this.scrollId = scrollId;
    }

    public ScrollFileSystemItemListImpl(String scrollId, List<FileSystemItem> list) {
        super(list);
        this.scrollId = scrollId;
    }

    @Override
    public String getScrollId() {
        return scrollId;
    }

    @Override
    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    @Override
    public String toString() {
        return String.format("scrollId = %s, fileSystemItems = %s", scrollId, super.toString());
    }
}
