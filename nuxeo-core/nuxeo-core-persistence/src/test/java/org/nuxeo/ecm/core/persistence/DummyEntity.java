/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
