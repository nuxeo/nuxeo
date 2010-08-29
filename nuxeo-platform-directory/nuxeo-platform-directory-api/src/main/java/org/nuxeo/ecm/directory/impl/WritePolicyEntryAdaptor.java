/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.directory.impl;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.EntryAdaptor;

/**
 * Simple entry adaptor implementation that leaves the entry as editable if the
 * specified field value matches the provided regexp or set the readonly flag of
 * the entry to true if the value of the field does not match the regexp.
 *
 * In any case, if the readonly flag of the adapted entry is already set to
 * true, this value is kept unchanged.
 */
public class WritePolicyEntryAdaptor implements EntryAdaptor {

    public static final Log log = LogFactory.getLog(WritePolicyEntryAdaptor.class);

    protected String fieldName;

    protected Pattern pattern;

    public DocumentModel adapt(Directory directory, DocumentModel entry)
            throws DirectoryException {
        if (fieldName == null || pattern == null) {
            log.warn(getClass().getName()
                    + " is missing configuration parameters");
            return entry;
        }
        if (BaseSession.isReadOnlyEntry(entry)) {
            // keep already existing flag
            return entry;
        }
        try {
            Object fieldValue = entry.getProperty(directory.getSchema(),
                    fieldName);
            String value = fieldValue != null ? fieldValue.toString() : "";
            if (pattern.matcher(value).matches()) {
                BaseSession.setReadWriteEntry(entry);
            } else {
                BaseSession.setReadOnlyEntry(entry);
            }
        } catch (ClientException e) {
            throw new DirectoryException(
                    "Field to adapt entry " + entry.getId()
                            + "  from directory " + directory.getName(), e);
        }
        return entry;
    }

    public void setParameter(String name, String value) {
        if ("fieldName".equals(name)) {
            this.fieldName = value;
        } else if ("regexp".equals(name)) {
            this.pattern = Pattern.compile(value);
        } else {
            log.warn("unexpected parameter " + name + " for class "
                    + getClass().getName());
        }
    }

}
