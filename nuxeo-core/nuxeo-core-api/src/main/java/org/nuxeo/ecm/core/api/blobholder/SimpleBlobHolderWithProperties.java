/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

public class SimpleBlobHolderWithProperties extends SimpleBlobHolder {

    protected final Map<String, Serializable> properties;

    public SimpleBlobHolderWithProperties(Blob blob, Map<String, Serializable> properties) {
        super(blob);
        this.properties = properties;
    }

    @Override
    public Serializable getProperty(String name) throws ClientException {
        if (properties == null) {
            return null;
        }
        return properties.get(name);
    }

    @Override
    public Map<String, Serializable> getProperties() {
        return properties;
    }

}
