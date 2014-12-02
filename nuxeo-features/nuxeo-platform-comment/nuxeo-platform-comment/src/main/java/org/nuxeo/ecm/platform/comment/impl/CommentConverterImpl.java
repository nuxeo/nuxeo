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
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.impl;

import java.util.HashMap;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.comment.api.CommentConverter;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentConverterImpl implements CommentConverter {

    public String getDocumentType() {
        return CommentsConstants.COMMENT_DOC_TYPE;
    }

    public void updateDocumentModel(DocumentModel docModel, DocumentModel comment) {

        DataModelMap dataModelMap = comment.getDataModels();
        Set<String> keys = dataModelMap.keySet();

        for(String key : keys) {
            DataModel dataModel = dataModelMap.get(key);
            String schema = dataModel.getSchema();
            try {
                docModel.setProperties(schema, new HashMap<String, Object>(dataModel.getMap()));
            } catch (ClientException e) {
                continue;
            }
        }
    }

}
