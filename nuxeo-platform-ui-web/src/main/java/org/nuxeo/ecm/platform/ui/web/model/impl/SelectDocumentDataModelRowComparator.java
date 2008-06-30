/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SelectDocumentDataModelRowComparator.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model.impl;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;

/**
 * Comparator for {@link org.nuxeo.ecm.platform.ui.web.model.SelectDataModel} items containing {@link DocumentModel}
 * data.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class SelectDocumentDataModelRowComparator
        implements Comparator<SelectDataModelRow>, Serializable {

    private static final long serialVersionUID = -5760751243552415990L;

    private static final Log log = LogFactory
            .getLog(SelectDocumentDataModelRowComparator.class);

    protected final String schemaName;

    protected final String fieldName;

    protected int ascending = -1;

    public SelectDocumentDataModelRowComparator(String schemaName,
            String fieldName, boolean ascending) {
        this.schemaName = schemaName;
        this.fieldName = fieldName;
        this.ascending = ascending ? 1 : -1;
    }

    /**
     * Compares two cells given two table rows.
     */
    public int compare(SelectDataModelRow row1, SelectDataModelRow row2) {
        int result = 0;
        try {
            DocumentModel d1 = (DocumentModel) row1.getData();
            DocumentModel d2 = (DocumentModel) row2.getData();

            if (d1 == null) {
                result = (d2 == null) ? 0 : -1;
            } else if (d2 == null) {
                result = 1;
            } else {
                // compare properties
                Object o1 = d1.getProperty(schemaName, fieldName);
                Object o2 = d2.getProperty(schemaName, fieldName);

                if (o1 == null) {
                    result = (o2 == null) ? 0 : -1;
                } else if (o2 == null) {
                    result = 1;
                } else {
                    if (o1 instanceof Comparable) {
                        result = ((Comparable) o1).compareTo(o2);
                    } else {
                        // default on string comparison
                        result = o1.toString().compareTo(o2.toString());
                    }
                }
            }
        } catch (Exception err) {
            log.error("Error when comparing rows " + err);
        }

        return result * ascending;
    }

}
