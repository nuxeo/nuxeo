/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.xml;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.connect.update.model.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject
public class FieldDefinition implements Field {

    @XNode("@name")
    protected String name;

    @XNode("@type")
    protected String type;

    @XNode("@required")
    protected boolean isRequired;

    @XNode("@vertical")
    protected boolean isVertical;

    @XNode("@readonly")
    protected boolean isReadOnly;

    @XNode("label")
    protected String label;

    @XNode("value")
    protected String value;

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public boolean isVertical() {
        return isVertical;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public String getValue() {
        return value;
    }

}
