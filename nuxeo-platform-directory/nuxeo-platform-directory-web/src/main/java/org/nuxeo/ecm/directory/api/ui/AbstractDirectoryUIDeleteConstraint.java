/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.api.ui;

import java.util.Map;

import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Abstract class handling storage of properties.
 * <p>
 * Can be subclassed to make code more robust to API changes.
 *
 * @author Anahide Tchertchian
 */
public abstract class AbstractDirectoryUIDeleteConstraint implements
        DirectoryUIDeleteConstraint {

    private static final long serialVersionUID = 1L;

    protected Map<String, String> properties;

    public void setProperties(Map<String, String> properties)
            throws DirectoryException {
        if (properties != null) {
            this.properties = properties;
        }
    }

}
