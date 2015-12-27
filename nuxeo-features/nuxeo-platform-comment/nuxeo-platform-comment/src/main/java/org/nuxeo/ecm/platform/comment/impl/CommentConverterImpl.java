/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.impl;

import java.util.HashMap;
import java.util.Set;

import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DataModelMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
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

        for (String key : keys) {
            DataModel dataModel = dataModelMap.get(key);
            String schema = dataModel.getSchema();
            try {
                docModel.setProperties(schema, new HashMap<String, Object>(dataModel.getMap()));
            } catch (PropertyException e) {
                continue;
            }
        }
    }

}
