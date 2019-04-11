/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.comment.impl;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.Sorter;

public class CommentSorter implements Sorter {

    private static final long serialVersionUID = 1L;

    private boolean asc = true;

    public CommentSorter(boolean asc) {
        this.asc = asc;
    }

    @Override
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
            Calendar created1 = (Calendar) doc1.getPropertyValue("dc:created");
            Calendar created2 = (Calendar) doc2.getPropertyValue("dc:created");

            if (created1 == null && created2 == null) {
                return 0;
            } else if (created1 == null) {
                return asc ? -1 : 1;
            } else if (created2 == null) {
                return asc ? 1 : -1;
            }
            cmp = created1.compareTo(created2);
        } catch (PropertyException e) {
        }
        return asc ? cmp : -cmp;
    }

}
