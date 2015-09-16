/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Dragos Mihalache
 */
package org.nuxeo.ecm.core.uidgen;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * The abstract class adds some useful methods.
 */
public abstract class AbstractUIDGenerator implements UIDGenerator {

    private static final Log log = LogFactory.getLog(AbstractUIDGenerator.class);

    private UIDSequencer sequencer;

    private String[] propertyNames;

    @Override
    public final void setSequencer(UIDSequencer sequencer) {
        if (null == sequencer) {
            throw new IllegalArgumentException("null sequencer");
        }
        this.sequencer = sequencer;
    }

    protected int getNext(final DocumentModel document) {
        if (null == sequencer) {
            throw new IllegalStateException("sequencer not defined");
        }
        final String key = getSequenceKey(document);
        assert key != null;
        return sequencer.getNext(key);
    }

    @Override
    public String getPropertyName() {
        if (propertyNames.length == 0) {
            log.warn("No propertyName specified");
            return null;
        }
        return propertyNames[0];
    }

    @Override
    public void setPropertyName(String propertyName) {
        propertyNames = new String[] { propertyName };
    }

    @Override
    public void setPropertyNames(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }

    @Override
    public String[] getPropertyNames() {
        return propertyNames;
    }

    /**
     * Checks if the property with the given name is defined and is not null.
     */
    protected final boolean isPropValueDefined(String propName, DocumentModel document) {
        try {
            Object val = document.getProperty(getSchemaName(propName), getFieldName(propName));
            return val != null;
        } catch (PropertyException e) {
            return false;
        }
    }

    protected final String str(String propName, DocumentModel document) {
        Object val = document.getProperty(getSchemaName(propName), getFieldName(propName));
        if (val == null) {
            return null;
        }
        if (val instanceof String) {
            return (String) val;
        }
        throw new NuxeoException("Doc property '" + propName + "' is not of String type.");
    }

    @Override
    public void setUID(DocumentModel document) throws PropertyNotFoundException {
        String uid = createUID(document);
        for (String propertyName : propertyNames) {
            try {
                document.setProperty(getSchemaName(propertyName), getFieldName(propertyName), uid);
            } catch (PropertyNotFoundException e) {
                e.addInfo(String.format("Cannot set uid %s on property %s for doc %s", uid, propertyName, document));
                throw e;
            }
        }
    }

    // helper method to deprecate
    private static String getSchemaName(String propertyName) {
        String[] s = propertyName.split(":");
        String prefix = s[0];
        SchemaManager tm = Framework.getService(SchemaManager.class);
        Schema schema = tm.getSchemaFromPrefix(prefix);
        if (schema == null) {
            // fall back on prefix as it may be the schema name
            return prefix;
        } else {
            return schema.getName();
        }
    }

    // helper method to deprecate
    private static String getFieldName(String propertyName) {
        String[] s = propertyName.split(":");
        return s[1];
    }

}
