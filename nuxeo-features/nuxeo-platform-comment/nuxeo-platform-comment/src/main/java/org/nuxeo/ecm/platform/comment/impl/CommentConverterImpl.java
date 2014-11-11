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

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class CommentConverterImpl implements CommentConverter {

    /*protected String getSchema() {
        return "comment";
    }*/

    /*protected Class getCommentClass() {
        return CommentImpl.class;
    }*/

    public String getDocumentType() {
        return "Comment";
    }

    /*public Comment getComment(DocumentModel docModel) throws ClientException {
        CommentImpl comment;
        try {
            comment = (CommentImpl) getCommentClass().newInstance();
            String schema = getSchema();

            comment.setId(docModel.getId());
            Map<String, Object> props = docModel.getProperties(schema);
            comment.setAuthor((String) props.get("author"));

            String content = (String) props.get("text");
            if(content!=null) {
                comment.setContent(content.trim());
            }

            Calendar cal = (Calendar) props.get("creationDate");
            if (cal != null) {
                comment.setCreationDate(cal.getTime());
            }
            Blob fileContent = (Blob)props.get("fileContent");
            if(fileContent!=null) {
                String filename = (String) props.get("filename");
                if(filename!=null) {
                    comment.setFileContent(fileContent);
                    comment.setFilename(filename);
                }

            }
        } catch (Exception e) {
            throw new ClientException("failed to create comment", e);
        }
        return comment;
    }*/

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
            /*if(docModel.hasSchema(schema)) {
                docModel.setProperties(schema,dataModel.getMap());
            }*/
        }
        /*docModel.setProperty(schema, "text", comment.getContent());
        docModel.setProperty(schema, "creationDate", comment.getCreationDate());
        docModel.setProperty(schema, "author", comment.getAuthor());
        docModel.setProperty(schema, "fileContent", comment.getFileContent());
        docModel.setProperty(schema, "filename", comment.getFilename());*/
    }

}
