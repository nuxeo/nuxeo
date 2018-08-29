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
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOCUMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT_FIELD;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class CommentJsonWriter extends ExtensibleEntityJsonWriter<Comment> {

    public CommentJsonWriter() {
        super(COMMENT_ENTITY_TYPE, Comment.class);
    }

    @Override
    protected void writeEntityBody(Comment entity, JsonGenerator jg) throws IOException {
        writeCommentEntity(entity, jg);
    }

    protected static void writeCommentEntity(Comment entity, JsonGenerator jg) throws IOException {
        jg.writeStringField(COMMENT_ID_FIELD, entity.getId());
        jg.writeStringField(COMMENT_DOCUMENT_ID_FIELD, entity.getDocumentId());
        jg.writeStringField(COMMENT_AUTHOR_FIELD, entity.getAuthor());
        jg.writeStringField(COMMENT_TEXT_FIELD, entity.getText());

        String creationDate = entity.getCreationDate() != null ? entity.getCreationDate().toString() : null;
        jg.writeStringField(COMMENT_CREATION_DATE_FIELD, creationDate);
        String modificationDate = entity.getModificationDate() != null ? entity.getModificationDate().toString() : null;
        jg.writeStringField(COMMENT_MODIFICATION_DATE_FIELD, modificationDate);

        if (entity instanceof ExternalEntity) {
            jg.writeStringField(EXTERNAL_ENTITY_ID, ((ExternalEntity) entity).getEntityId());
            jg.writeStringField(EXTERNAL_ENTITY_ORIGIN, ((ExternalEntity) entity).getOrigin());
            jg.writeStringField(EXTERNAL_ENTITY, ((ExternalEntity) entity).getEntity());
        }
    }
}
