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

import java.util.HashMap;
import java.util.Map;

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

import org.hibernate.annotations.CollectionOfElements;
import org.nuxeo.theme.webwidgets.Widget;
import org.nuxeo.theme.webwidgets.WidgetState;

@Entity
@Table(name = "NXP_WEBWIDGETS_WIDGET")
@NamedQueries( {
        @NamedQuery(name = "Widget.findAll", query = "FROM WidgetEntity widget WHERE widget.region=:region ORDER BY widget.order"),
        @NamedQuery(name = "Widget.findByScope", query = "FROM WidgetEntity widget WHERE widget.region=:region AND widget.scope=:scope ORDER BY widget.order") })
public class WidgetEntity implements Widget {

    protected int id;

    protected String uid;

    protected String name;

    protected String region;

    protected WidgetState state;

    protected int order;

    protected Map<String, String> preferences = new HashMap<String, String>();

    protected String scope;

    public WidgetEntity() {
    }

    public WidgetEntity(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "WIDGET_ID", nullable = false, columnDefinition = "integer")
    public int getId() {
        return id;
    }

    @Transient
    public String getUid() {
        return String.valueOf(id);
    }

    @Column(name = "WIDGET_NAME")
    public String getName() {
        return name;
    }

    @Column(name = "WIDGET_REGION")
    public String getRegion() {
        return region;
    }

    @Column(name = "WIDGET_ORDER", columnDefinition = "integer")
    public int getOrder() {
        return order;
    }

    @Column(name = "WIDGET_STATE")
    public WidgetState getState() {
        return state;
    }

    @CollectionOfElements(fetch = FetchType.EAGER)
    @Column(name = "WIDGET_PREFERENCES")
    public Map<String, String> getPreferences() {
        return new HashMap<String, String>(preferences);
    }

    @Column(name = "WIDGET_SCOPE")
    public String getScope() {
        return scope;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setState(WidgetState state) {
        this.state = state;
    }

    public void setPreferences(Map<String, String> preferences) {
        this.preferences = preferences;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

}
