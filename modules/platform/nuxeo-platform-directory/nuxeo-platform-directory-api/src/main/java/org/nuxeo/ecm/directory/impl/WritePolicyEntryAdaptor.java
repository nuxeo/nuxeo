/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.directory.impl;

import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.EntryAdaptor;

/**
 * Simple entry adaptor implementation that leaves the entry as editable if the specified field value matches the
 * provided regexp or set the readonly flag of the entry to true if the value of the field does not match the regexp.
 * <p>
 * In any case, if the readonly flag of the adapted entry is already set to true, this value is kept unchanged.
 */
public class WritePolicyEntryAdaptor implements EntryAdaptor {

    public static final Log log = LogFactory.getLog(WritePolicyEntryAdaptor.class);

    protected String fieldName;

    protected Pattern pattern;

    @Override
    public DocumentModel adapt(Directory directory, DocumentModel entry) {
        if (fieldName == null || pattern == null) {
            log.warn(getClass().getName() + " is missing configuration parameters");
            return entry;
        }
        if (BaseSession.isReadOnlyEntry(entry)) {
            // keep already existing flag
            return entry;
        }
        try {
            Object fieldValue = entry.getProperty(directory.getSchema(), fieldName);
            String value = fieldValue != null ? fieldValue.toString() : "";
            if (pattern.matcher(value).matches()) {
                BaseSession.setReadWriteEntry(entry);
            } else {
                BaseSession.setReadOnlyEntry(entry);
            }
        } catch (PropertyException e) {
            throw new DirectoryException(
                    String.format(
                            "The field '%s' of entry '%s' could not be adapt and map on directory '%s', check that the field exist in the schema",
                            fieldName, entry.getId(), directory.getName()), e);

        }
        return entry;
    }

    @Override
    public void setParameter(String name, String value) {
        if ("fieldName".equals(name)) {
            fieldName = value;
        } else if ("regexp".equals(name)) {
            pattern = Pattern.compile(value);
        } else {
            log.warn("unexpected parameter " + name + " for class " + getClass().getName());
        }
    }

}
