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

package org.nuxeo.ecm.webapp.table.cell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * A table cell that knows do display a property from a DocumentModel.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class DocModelTableCell extends AbstractTableCell {

    private static final long serialVersionUID = 661302415901705399L;

    private static final Log log = LogFactory.getLog(DocModelTableCell.class);

    protected DocumentModel doc;

    protected final String schemaName;

    protected final String propertyName;

    public DocModelTableCell(DocumentModel doc, String schema, String property) {
        this.doc = doc;
        schemaName = schema;
        propertyName = property;

        log.debug("DocModelTableCell created: " + schema + ", " + property);
    }

    @Override
    public Object getValue() {
        return doc;
    }

    @Override
    public Object getDisplayedValue() {
        try {
            return doc.getProperty(schemaName, propertyName);
        } catch (ClientException e) {
            return null;
        }
    }

    @Override
    public void setDisplayedValue(Object o) {
        try {
            doc.setProperty(schemaName, propertyName, o);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    @Override
    public void setValue(Object value) {
        doc = (DocumentModel) value;
    }

    public int compareTo(AbstractTableCell cell) {

        String dispValue = (String) getDisplayedValue();
        if (null != cell && null != dispValue) {
            return dispValue.compareTo((String) cell.getDisplayedValue());
        } else {
            return 0;
        }
    }

}
