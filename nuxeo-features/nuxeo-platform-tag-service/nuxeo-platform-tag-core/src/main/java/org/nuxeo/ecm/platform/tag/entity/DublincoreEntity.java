/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.nuxeo.ecm.platform.tag.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Generated for the Dublincore table.
 */
@Entity
@Table(name = "DUBLINCORE")
public class DublincoreEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Column(name = "VALID")
    @Temporal(TemporalType.TIMESTAMP)
    private Date valid;

    @Column(name = "ISSUED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date issued;

    @Lob
    @Column(name = "COVERAGE")
    private String coverage;

    @Lob
    @Column(name = "TITLE")
    private String title;

    @Column(name = "MODIFIED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modified;

    @Lob
    @Column(name = "CREATOR")
    private String creator;

    @Lob
    @Column(name = "RIGHTS")
    private String rights;

    @Lob
    @Column(name = "LANGUAGE")
    private String language;

    @Column(name = "EXPIRED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expired;

    @Column(name = "CREATED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Lob
    @Column(name = "SOURCE")
    private String source;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    @Lob
    @Column(name = "FORMAT")
    private String format;

    @JoinColumn(name = "ID", referencedColumnName = "ID", insertable = false, updatable = false)
    @OneToOne
    private HierarchyEntity hierarchy;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "targetDocument")
    private List<TaggingEntity> taggings;

    public DublincoreEntity() {
    }

    public DublincoreEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getValid() {
        return valid;
    }

    public void setValid(Date valid) {
        this.valid = valid;
    }

    public Date getIssued() {
        return issued;
    }

    public void setIssued(Date issued) {
        this.issued = issued;
    }

    public String getCoverage() {
        return coverage;
    }

    public void setCoverage(String coverage) {
        this.coverage = coverage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getRights() {
        return rights;
    }

    public void setRights(String rights) {
        this.rights = rights;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Date getExpired() {
        return expired;
    }

    public void setExpired(Date expired) {
        this.expired = expired;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public HierarchyEntity getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(HierarchyEntity hierarchy) {
        this.hierarchy = hierarchy;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are
        // not set
        if (!(object instanceof DublincoreEntity)) {
            return false;
        }
        DublincoreEntity other = (DublincoreEntity) object;
        if ((this.id == null && other.id != null)
                || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "javaapplication2.Dublincore[id=" + id + "]";
    }

    public void setTaggings(List<TaggingEntity> taggings) {
        this.taggings = taggings;
    }

    public List<TaggingEntity> getTaggings() {
        if (taggings == null) {
            taggings = new LinkedList<TaggingEntity>();
        }
        return taggings;
    }

}
