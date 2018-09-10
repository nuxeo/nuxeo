/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT_FIELD;

import java.time.Instant;

import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class CommentJsonReader extends EntityJsonReader<Comment> {

    public CommentJsonReader() {
        super(COMMENT_ENTITY_TYPE);
    }

    @Override
    protected Comment readEntity(JsonNode jn) {
        Comment comment = new CommentImpl();
        return fillCommentEntity(jn, comment);

    }

    protected static Comment fillCommentEntity(JsonNode jn, Comment comment) {
        // don't read id from given JsonNode, if needed it is read from path
        comment.setParentId(jn.get(COMMENT_PARENT_ID_FIELD).textValue());
        comment.setAuthor(jn.get(COMMENT_AUTHOR_FIELD).textValue());
        comment.setText(jn.get(COMMENT_TEXT_FIELD).textValue());

        Instant creationDate = jn.get(COMMENT_CREATION_DATE_FIELD).textValue() != null
                ? Instant.parse(jn.get(COMMENT_CREATION_DATE_FIELD).textValue())
                : null;
        comment.setCreationDate(creationDate);
        Instant modificationDate = jn.get(COMMENT_MODIFICATION_DATE_FIELD).textValue() != null
                ? Instant.parse(jn.get(COMMENT_MODIFICATION_DATE_FIELD).textValue())
                : null;
        comment.setModificationDate(modificationDate);

        if (jn.has(EXTERNAL_ENTITY_ID)) {
            ExternalEntity externalEntity = (ExternalEntity) comment;
            externalEntity.setEntityId(jn.get(EXTERNAL_ENTITY_ID).textValue());
            externalEntity.setOrigin(jn.get(EXTERNAL_ENTITY_ORIGIN).textValue());
            externalEntity.setEntity(jn.get(EXTERNAL_ENTITY).textValue());
        }
        return comment;
    }
}
