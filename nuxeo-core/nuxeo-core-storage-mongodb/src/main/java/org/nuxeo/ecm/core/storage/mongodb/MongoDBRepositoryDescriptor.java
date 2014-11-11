/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * MongoDB Repository Descriptor.
 */
@XObject(value = "repository")
public class MongoDBRepositoryDescriptor {

    public MongoDBRepositoryDescriptor() {
    }

    @XNode("@name")
    public String name;

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
    }

    // ----- MongoDB specific options -----

    @XNode("server")
    public String server;

    /** Copy constructor. */
    public MongoDBRepositoryDescriptor(MongoDBRepositoryDescriptor other) {
        name = other.name;
        label = other.label;
        isDefault = other.isDefault;
        server = other.server;
    }

    public void merge(MongoDBRepositoryDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.label != null) {
            label = other.label;
        }
        if (other.isDefault != null) {
            isDefault = other.isDefault;
        }
        if (other.server != null) {
            server = other.server;
        }
    }

}
