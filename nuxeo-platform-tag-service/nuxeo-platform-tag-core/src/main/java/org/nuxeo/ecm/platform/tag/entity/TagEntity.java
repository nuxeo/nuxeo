/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.nuxeo.ecm.platform.tag.entity;

import java.io.Serializable;
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

/**
 * Generated for Tag table.
 */
@Entity
@Table(name = "TAG")
public class TagEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Column(name = "PRIVATE")
    private boolean private1;

    @Lob
    @Column(name = "LABEL")
    private String label;

    @JoinColumn(name = "ID", referencedColumnName = "ID", insertable = false, updatable = false)
    @OneToOne
    private HierarchyEntity hierarchy;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tag")
    private List<TaggingEntity> taggings;

    public TagEntity() {
    }

    public TagEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean getPrivate1() {
        return private1;
    }

    public void setPrivate1(boolean private1) {
        this.private1 = private1;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
        if (!(object instanceof TagEntity)) {
            return false;
        }
        TagEntity other = (TagEntity) object;
        if ((this.id == null && other.id != null)
                || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("Tag: id - ");
        ret.append(id);
        ret.append("; label - ");
        ret.append(label);
        return ret.toString();
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
