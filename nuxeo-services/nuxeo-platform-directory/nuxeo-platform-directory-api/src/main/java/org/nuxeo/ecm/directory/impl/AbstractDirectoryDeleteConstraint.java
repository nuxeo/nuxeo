/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.impl;

import java.util.Map;

import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;

/**
 * Abstract class handling storage of properties.
 * <p>
 * Can be subclassed to make code more robust to API changes.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractDirectoryDeleteConstraint implements DirectoryDeleteConstraint {

    private static final long serialVersionUID = 1L;

    protected Map<String, String> properties;

    @Override
    public void setProperties(Map<String, String> properties) {
        if (properties != null) {
            this.properties = properties;
        }
    }

}
