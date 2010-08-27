/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory.api.ui;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Directory ui descriptor
 *
 * @author Anahide Tchertchian
 *
 */
@XObject("deleteConstraint")
public class DirectoryUIDeleteConstraintDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<? extends DirectoryUIDeleteConstraint> klass;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties = new HashMap<String, String>();

    public Class<? extends DirectoryUIDeleteConstraint> getKlass() {
        return klass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public DirectoryUIDeleteConstraint getDeleteConstraint()
            throws DirectoryException {
        try {
            DirectoryUIDeleteConstraint instance = klass.newInstance();
            if (properties != null) {
                instance.setProperties(properties);
            }
            return instance;
        } catch (InstantiationException e) {
            throw new DirectoryException(e);
        } catch (IllegalAccessException e) {
            throw new DirectoryException(e);
        }
    }

}
