/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */
package org.nuxeo.webengine.sites.models;

import org.nuxeo.theme.models.AbstractModel;

/**
 * Model related to comments that are bounded to a <b>WebPage</b> in the
 * fragment initialization mechanism
 *
 * @author rux
 */
public class CommentModel extends AbstractModel {

    private String commentText;

    private String ref;

    private String author;

    private String creationDate;

    private String siteUrl;

    private boolean pendingComment;

    public CommentModel(String creationDate, String author, String commentText,
            String ref, boolean pendingComment) {
        this.creationDate = creationDate;
        this.author = author;
        this.commentText = commentText;
        this.ref = ref;
        this.pendingComment = pendingComment;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isPendingComment() {
        return pendingComment;
    }

    public void setPendingComment(boolean pendingComment) {
        this.pendingComment = pendingComment;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

}
