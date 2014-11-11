/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.server.test.business.client;

import org.nuxeo.ecm.automation.client.annotations.EntityType;

/**
 * Automation client File pojo example - Annotated by EntityType setting the
 * document model adapter simple name to map server side
 */
@EntityType("BusinessBeanAdapter")
public class BusinessBean {

    protected String title;

    protected String description;

    protected String id;

    protected String note;

    protected String type;

    protected Object object;

    public BusinessBean() {
    }

    public BusinessBean(String title, String description, String note,
            String type, Object object) {
        this.title = title;
        this.description = description;
        this.note = note;
        this.type = type;
        this.object = object;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
