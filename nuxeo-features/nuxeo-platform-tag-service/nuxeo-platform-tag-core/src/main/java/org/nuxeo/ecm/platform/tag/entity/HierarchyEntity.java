/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.nuxeo.ecm.platform.tag.entity;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Generated for Hierarchy table.
 */
@Entity
@Table(name = "HIERARCHY")
public class HierarchyEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", nullable = false)
    private String id;

    @Column(name = "POS")
    private Integer pos;

    @Column(name = "NAME")
    private String name;

    @Column(name = "PRIMARYTYPE")
    private String primarytype;

    @Column(name = "BASEVERSIONID")
    private String baseversionid;

    @Column(name = "MAJORVERSION")
    private Integer majorversion;

    @Column(name = "MINORVERSION")
    private Integer minorversion;

    @OneToMany(mappedBy = "parentid")
    private Collection<HierarchyEntity> hierarchyCollection;

    @JoinColumn(name = "PARENTID", referencedColumnName = "ID")
    @ManyToOne
    private HierarchyEntity parentid;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "hierarchy")
    private DublincoreEntity dublincore;

//    @OneToOne(cascade = CascadeType.ALL, mappedBy = "hierarchy")
//    private TagEntity tag;

    public HierarchyEntity() {
    }

    public HierarchyEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPos() {
        return pos;
    }

    public void setPos(Integer pos) {
        this.pos = pos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimarytype() {
        return primarytype;
    }

    public void setPrimarytype(String primarytype) {
        this.primarytype = primarytype;
    }

    public String getBaseversionid() {
        return baseversionid;
    }

    public void setBaseversionid(String baseversionid) {
        this.baseversionid = baseversionid;
    }

    public Integer getMajorversion() {
        return majorversion;
    }

    public void setMajorversion(Integer majorversion) {
        this.majorversion = majorversion;
    }

    public Integer getMinorversion() {
        return minorversion;
    }

    public void setMinorversion(Integer minorversion) {
        this.minorversion = minorversion;
    }

    public Collection<HierarchyEntity> getHierarchyCollection() {
        return hierarchyCollection;
    }

    public void setHierarchyCollection(Collection<HierarchyEntity> hierarchyCollection) {
        this.hierarchyCollection = hierarchyCollection;
    }

    public HierarchyEntity getParentid() {
        return parentid;
    }

    public void setParentid(HierarchyEntity parentid) {
        this.parentid = parentid;
    }

    public DublincoreEntity getDublincore() {
        return dublincore;
    }

    public void setDublincore(DublincoreEntity dublincore) {
        this.dublincore = dublincore;
    }

//    public TagEntity getTag() {
//        return tag;
//    }
//
//    public void setTag(TagEntity tag) {
//        this.tag = tag;
//    }

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
        if (!(object instanceof HierarchyEntity)) {
            return false;
        }
        HierarchyEntity other = (HierarchyEntity) object;
        if ((this.id == null && other.id != null)
                || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Hierarchy[id=" + id + "]";
    }

}
