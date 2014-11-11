/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Brice Chaffangeon
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.syndication;

import java.util.Date;
import java.util.List;

import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntryImpl;

/**
 * @author Brice Chaffangeon
 * @author Florent Guillaume
 */
public class FeedItem extends SyndEntryImpl implements Comparable<FeedItem> {

    private static final long serialVersionUID = 1L;

    /**
     * Set the description.
     * <p>
     * This overloads {@link SyndEntryImpl#setDescription} in order to avoid a
     * NPE when description is null.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        SyndContentImpl content = new SyndContentImpl();
        content.setType("text/plain");
        if (description == null || "".equals(description)) {
            description = " ";
        }
        content.setValue(description);
        super.setDescription(content);
    }

    @Override
    public void setAuthor(String author) {
        if (author != null && !"".equals(author)) {
            super.setAuthor(author);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    // Can't use List<String> here because we're overriding an external library.
    public void setContributors(List contributors) {
        if (contributors != null && !contributors.isEmpty()) {
            super.setContributors(contributors);
        }
    }

    @Override
    public void setTitle(String title) {
        if (title != null && !"".equals(title)) {
            super.setTitle(title);
        }
    }

    /**
     * @deprecated Unused
     */
    @Deprecated
    public void setTitle(String title, String type) {
        if ("".equals(type)) {
            setTitle(title);
        } else {
            setTitle(title + " - [" + type + ']');
        }
    }

    @Override
    public void setLink(String link) {
        if (link != null && !"".equals(link)) {
            super.setLink(link);
        }
    }

    @Override
    public void setPublishedDate(Date publishedDate) {
        if (publishedDate != null) {
            super.setPublishedDate(publishedDate);
        }
    }

    @Override
    public void setUpdatedDate(Date updatedDate) {
        if (updatedDate != null) {
            super.setUpdatedDate(updatedDate);
        }
    }

    public int compareTo(FeedItem fi) {
        if (getUpdatedDate() != null && fi.getUpdatedDate() != null) {
            return getUpdatedDate().compareTo(fi.getUpdatedDate());
        } else {
            return getPublishedDate().compareTo(fi.getPublishedDate());
        }
    }

}
