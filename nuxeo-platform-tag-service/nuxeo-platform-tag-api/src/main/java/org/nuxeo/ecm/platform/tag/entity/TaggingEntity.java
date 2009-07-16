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
 */

package org.nuxeo.ecm.platform.tag.entity;

import static org.nuxeo.ecm.platform.tag.entity.TaggingConstants.*;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Tagging entry. Table structure:
 * <p>NUMBER TAGGING_ID //the PK ID
 * <p>STRING TARGET_ID // FK, the tagged document ID
 * <p>STRING TAG_ID // FK, the tag ID applied on document
 * <p>STRING AUTHOR // the user name applying the tag
 * <p>DATE CREATION_DATE // the time of creation of the tagging (apply of the tag)
 * <p>BOOL IS_PRIVATE // default false, marks a tagging as private
 * <p>Private tagging means the respective tag applying is visible only for the
 * creator. Mainly it impacts the tag clouding computation.
 *
 * @author cpriceputu
 */
@Entity(name = "Tagging")
@Table(name = "nxp_tagging")
@NamedQueries( {
        @NamedQuery(name = LIST_TAGS_FOR_DOCUMENT, query = LIST_TAGS_FOR_DOCUMENT_QUERY),
        @NamedQuery(name = LIST_TAGS_FOR_DOCUMENT_AND_USER, query = LIST_TAGS_FOR_DOCUMENT_AND_USER_QUERY),
        @NamedQuery(name = GET_VOTE_TAG, query = GET_VOTE_TAG_QUERY),
        @NamedQuery(name = REMOVE_TAGGING, query = REMOVE_TAGGING_QUERY),
        @NamedQuery(name = GET_VOTE_CLOUD, query = GET_VOTE_CLOUD_QUERY),
        @NamedQuery(name = LIST_DOCUMENTS_FOR_TAG, query = LIST_DOCUMENTS_FOR_TAG_QUERY),
        @NamedQuery(name = GET_TAGGING, query = GET_TAGGING_QUERY)

})
public class TaggingEntity implements Serializable{

    private static final long serialVersionUID = -1091703195187974444L;

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "DOCUMENT_ID", nullable = false, updatable = false)
    private DublincoreEntity targetDocument;

    /**
     * Returns the identifier of the tag.
     *
     * @return the tag identifier
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "TAG_ID", nullable = false, updatable = false)
    private TagEntity tag;

    /**
     * Returns the author of the tagging.
     *
     * @return the author of the tagging
     */
    @Column(name = "AUTHOR")
    private String author;

    /**
     * Returns the date when the tagging was made.
     *
     * @return the date when the tagging was made
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATION_DATE")
    private Date creationDate;

    /**
     * A flag marking indicating the tagging is available for everyone or only
     * for creator and administrators
     *
     * @return true the tagging is available for everyone, false if only for
     *         creator and administrators
     */
    @Column(name = "IS_PRIVATE")
    private Boolean isPrivate = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setTargetDocument(DublincoreEntity targetDocument) {
        this.targetDocument = targetDocument;
    }

    public DublincoreEntity getTargetDocument() {
        return targetDocument;
    }

    public void setTag(TagEntity tag) {
        this.tag = tag;
    }

    public TagEntity getTag() {
        return tag;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("Tagging: id - ");
        ret.append(id);
        ret.append("; target - ");
        ret.append(targetDocument.getTitle());
        ret.append("; tag - ");
        ret.append(tag.getLabel());
        return ret.toString();
    }

}
