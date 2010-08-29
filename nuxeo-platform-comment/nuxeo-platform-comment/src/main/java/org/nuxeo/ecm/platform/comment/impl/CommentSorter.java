/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.comment.impl;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Sorter;

public class CommentSorter implements Sorter {

    private static final long serialVersionUID = 1L;

    private boolean asc = true;

    public CommentSorter(boolean asc) {
        this.asc = asc;
    }

    public int compare(DocumentModel doc1, DocumentModel doc2) {

         if (doc1 == null && doc2 == null) {
             return 0;
         } else if (doc1 == null) {
             return asc ? -1 : 1;
         } else if (doc2 == null) {
             return asc ? 1 : -1;
         }

        int cmp = 0;
        try {
            Calendar created1 = doc1.getProperty("dc:created").getValue(Calendar.class);
            Calendar created2 = doc2.getProperty("dc:created").getValue(Calendar.class);

            if (created1 == null && created2 == null) {
                return 0;
            } else if (created1 == null) {
                return asc ? -1 : 1;
            } else if (created2 == null) {
                return asc ? 1 : -1;
            }
            cmp = created1.compareTo(created2);
        }
        catch (Exception e) {
        }
        return asc ? cmp : -cmp;
    }

}
