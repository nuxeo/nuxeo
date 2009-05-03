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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.repository.jcr.properties;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.repository.jcr.JCRNodeProxy;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
class DateProperty extends JCRScalarProperty  {

    DateProperty(JCRNodeProxy parent, Property property, Field field) {
        super(parent, property, field);
    }

    @Override
    protected Property create(Object value) throws DocumentException {
        try {
            Calendar date = null;
            if (value instanceof Date) {
                date = Calendar.getInstance();
                date.setTime((Date) value);
            } else if (value instanceof Calendar) {
                date = (Calendar) value;
            }
            return parent.getNode().setProperty(
                    field.getName().getPrefixedName(), date);
        } catch (RepositoryException e) {
            throw new DocumentException("failed to set date property "
                    + field.getName(), e);
        }
    }

    @Override
    protected void set(Object value) throws RepositoryException {
        Calendar date = null;
        if (value instanceof Date) {
            date = Calendar.getInstance();
            date.setTime((Date) value);
        } else if (value instanceof Calendar) {
            date = (Calendar) value;
        }
        property.setValue(date);
    }

    @Override
    protected Object get() throws RepositoryException {
        return property.getDate();
    }
}
