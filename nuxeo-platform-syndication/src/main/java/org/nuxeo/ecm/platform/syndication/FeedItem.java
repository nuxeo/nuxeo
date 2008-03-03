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
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.syndication;

import java.util.Date;
import java.util.List;

import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntryImpl;

/**
 *
 * @author Brice Chaffangeon
 */
@SuppressWarnings("serial")
public class FeedItem extends SyndEntryImpl implements Comparable {

    /**
     * This overrides the SyndEntryImpl.setDescription, in order to avoid NPE
     * when description is null.
     *
     * @param description
     */
    public void setDescription(String description) {
        SyndContentImpl content = new SyndContentImpl();
        content.setType("text/plain");
        if (description != null && !"".equals(description)) {
            content.setValue(description);
        } else {
            content.setValue(" ");
        }
        super.setDescription(content);
    }

    @Override
    public void setAuthor(String author) {
        if (!"".equals(author)) {
            super.setAuthor(author);
        }
    }

    @Override
    // Can't use List<String> here because we're overriding an external library.
    public void setContributors(List contributors) {
        if (contributors != null && !contributors.isEmpty()) {
            super.setContributors(contributors);
        }
    }

    @Override
    public void setTitle(String title) {
        if (!"".equals(title)) {
            super.setTitle(title);
        }
    }

    public void setTitle(String title, String type) {
        if ("".equals(type)) {
            setTitle(title);
        } else {
            setTitle(title + " - [" + type + ']');
        }
    }

    @Override
    public void setLink(String link) {
        if (!"".equals(link)) {
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

    public int compareTo(Object o) {
        FeedItem fi = (FeedItem) o;
        int compare = 0;
        if (getUpdatedDate() != null && fi.getUpdatedDate() != null) {
            compare = getUpdatedDate().compareTo(fi.getUpdatedDate());
        } else {
            compare = getPublishedDate().compareTo(fi.getPublishedDate());
        }

        return compare;
    }

}
