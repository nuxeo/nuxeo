/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id: MemoryDirectory.java 30381 2008-02-20 20:12:09Z gracinet $
 */

package org.nuxeo.ecm.directory.memory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.IdGenerator;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 *
 */
public class MemoryDirectory extends AbstractDirectory {

    public final String name;

    public final String schemaName;

    public final Set<String> schemaSet;

    public final String idField;

    public final String passwordField;

    public Map<String, Object> map;

    public MemoryDirectorySession session;

    protected boolean isReadOnly = false;

    public MemoryDirectory(String name, String schema, String idField,
            String passwordField) throws DirectoryException {
        this(name, schema, new HashSet<String>(), idField, passwordField);

        SchemaManager sm = getSchemaManager();
        Schema sch = sm.getSchema(schema);
        if (sch == null) {
            throw new DirectoryException("Unknown schema :" + schema);
        }
        Collection<Field> fields = sch.getFields();
        for (Field f : fields) {
            schemaSet.add(f.getName().getLocalName());
        }
    }

    public SchemaManager getSchemaManager() throws DirectoryException {
        SchemaManager sm;
        try {
            sm = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw new DirectoryException("Unable to look up Core Type Service", e);
        }
        if (sm == null) {
            throw new DirectoryException("Unable to look up type service");
        }
        return sm;
    }

    public MemoryDirectory(String name, String schemaName, Set<String> schemaSet,
            String idField, String passwordField) {
        this.name = name;
        this.schemaName = schemaName;
        this.schemaSet = schemaSet;
        this.idField = idField;
        this.passwordField = passwordField;
    }

    public IdGenerator getIdGenerator() {
        return null;
    }

    public String getName() {
        return name;
    }

    public String getSchema() {
        return schemaName;
    }

    public String getParentDirectory() {
        return null;
    }

    public String getIdField() {
        return idField;
    }

    public String getPasswordField() {
        return passwordField;
    }

    public Session getSession() {
        if (session == null) {
            session = new MemoryDirectorySession(this);
        }
        return session;
    }

    public void shutdown() {
        session = null;
    }

    public boolean isReadOnly() {
        return isReadOnly ;
    }

    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

}
