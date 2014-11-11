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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl.osm;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScalarMemberProperty extends ScalarProperty {

    private static final long serialVersionUID = 1537310098432620929L;

    public ScalarMemberProperty(Property parent, Field field) {
        super (parent, field);
    }

    public ScalarMemberProperty(Property parent, Field field, int flags) {
        super (parent, field, flags);
    }


    @Override
    public void internalSetValue(Serializable value) throws PropertyException {
        ObjectAdapter adapter = ((Adaptable)parent).getAdapter();
        adapter.setValue(parent.getValue(), getName(), value);
    }

    @Override
    public Serializable internalGetValue() throws PropertyException {
        ObjectAdapter adapter = ((Adaptable)parent).getAdapter();
        return (Serializable)adapter.getValue(parent.getValue(), getName());
    }

}
