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
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

/**
 * MongoDB Repository Descriptor.
 */
@XObject(value = "repository")
public class MongoDBRepositoryDescriptor extends DBSRepositoryDescriptor {

    public MongoDBRepositoryDescriptor() {
    }

    @XNode("server")
    public String server;

    @XNode("dbname")
    public String dbname;

    @Override
    public MongoDBRepositoryDescriptor clone() {
        return (MongoDBRepositoryDescriptor) super.clone();
    }

    public void merge(MongoDBRepositoryDescriptor other) {
        super.merge(other);
        if (other.server != null) {
            server = other.server;
        }
        if (other.dbname != null) {
            dbname = other.dbname;
        }
    }

}
