/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Dummy entity implementation to store using Hibernate.
 */
@Entity(name = "DummyEntity")
@Table(name = "DUMMYENTITY")
public class DummyEntity {

    private String id;

    public DummyEntity() {
    }

    public DummyEntity(String id) {
        setId(id);
    }

    @Id
    @Column(name = "ENTITYID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
