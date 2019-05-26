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
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;

/**
 * Holds needed information about a content view to select it from UI, or to selected one of its result layouts from UI
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewHeader implements Serializable, Comparable<ContentViewHeader> {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String title;

    protected boolean translateTitle;

    protected String iconPath;

    public ContentViewHeader(String name, String title, boolean translateTitle, String iconPath) {
        this.name = name;
        this.title = title;
        this.translateTitle = translateTitle;
        this.iconPath = iconPath;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the title or the name if title is empty.
     */
    public String getTitle() {
        if (title == null || title.trim().isEmpty()) {
            return name;
        }
        return title;
    }

    public boolean isTranslateTitle() {
        return translateTitle;
    }

    public String getIconPath() {
        return iconPath;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof ContentViewHeader)) {
            return false;
        }

        ContentViewHeader otherContentViewHeader = (ContentViewHeader) other;
        return name == null ? otherContentViewHeader.name == null : name.equals(otherContentViewHeader.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(ContentViewHeader o) {
        if (o == null) {
            return 1;
        }
        String name1 = name;
        String name2 = o.name;
        if (name1 == null && name2 == null) {
            return 0;
        }
        if (name1 == null) {
            return -1;
        }
        if (name2 == null) {
            return 1;
        }
        return name1.compareTo(name2);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("ContentViewHeader")
           .append(" {")
           .append(" name=")
           .append(name)
           .append(", title=")
           .append(title)
           .append(", translateTitle=")
           .append(translateTitle)
           .append(", iconPath=")
           .append(iconPath)
           .append('}');
        return buf.toString();
    }

}
