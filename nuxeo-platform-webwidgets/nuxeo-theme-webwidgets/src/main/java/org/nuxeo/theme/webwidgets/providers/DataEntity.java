/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets.providers;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.nuxeo.theme.webwidgets.WidgetData;

@Entity
@Table(name = "NXP_WEBW_DATA")
@NamedQueries( {
        @NamedQuery(name = "Data.findByWidget", query = "FROM DataEntity data WHERE data.widgetUid=:widgetUid"),
        @NamedQuery(name = "Data.findByWidgetAndName", query = "FROM DataEntity data WHERE data.widgetUid=:widgetUid AND data.dataName=:dataName") })
public class DataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected int id;

    protected String widgetUid;

    protected String dataName;

    protected String filename;

    protected String contentType;

    protected byte[] content;

    public DataEntity() {
    }

    public DataEntity(String widgetUid, String dataName) {
        this.widgetUid = widgetUid;
        this.dataName = dataName;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", nullable = false, columnDefinition = "integer")
    public int getId() {
        return id;
    }

    @Column(name = "WIDGET_UID")
    public String getWidgetUid() {
        return widgetUid;
    }

    @Column(name = "NAME")
    public String getDataName() {
        return dataName;
    }

    @Column(name = "FILENAME")
    public String getFilename() {
        return filename;
    }

    @Column(name = "CONTENT_TYPE")
    public String getContentType() {
        return contentType;
    }

    /*
     * See http://opensource.atlassian.com/projects/hibernate/browse/HHH-2614
     */
    @Column(name = "CONTENT_DATA", length = Integer.MAX_VALUE - 1)
    @Basic(fetch = FetchType.LAZY)
    public byte[] getContent() {
        return content;
    }

    @Transient
    public WidgetData getData() {
        return new WidgetData(contentType, filename, content);
    }

    @Transient
    public void setData(WidgetData data) {
        filename = data.getFilename();
        content = data.getContent();
        contentType = data.getContentType();
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setWidgetUid(String widgetUid) {
        this.widgetUid = widgetUid;
    }

    public void setDataName(String dataName) {
        this.dataName = dataName;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

}
