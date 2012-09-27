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
 *     Thomas Roger (troger@nuxeo.com)
 */

package org.nuxeo.ecm.platform.picture.api;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Object to store the definition of a picture template, to be used when
 * computing views for a given image.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class PictureTemplate {

    protected final String title;

    protected final String description;

    protected final String tag;

    protected final int maxSize;

    public PictureTemplate(String title, String description, String tag,
            int maxSize) {
        this.title = title;
        this.description = description;
        this.tag = tag;
        this.maxSize = maxSize;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTag() {
        return tag;
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return String.format(
                "PictureTemplate [title=%s, description=%s, tag=%s, maxSize=%d]",
                title, description, tag, maxSize);
    }
}
