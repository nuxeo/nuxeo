/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *       Nuno Cunha <ncunha@nuxeo.com>
 */
package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_CREATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_MODIFICATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_FIELD;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_FIELD;

import java.time.Instant;
import java.util.function.Consumer;

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
        setIfExist(jn, COMMENT_PARENT_ID_FIELD, comment::setParentId);
        setIfExist(jn, COMMENT_TEXT_FIELD, comment::setText);
        setIfExist(jn, COMMENT_CREATION_DATE_FIELD, s -> comment.setCreationDate(s == null ? null : Instant.parse(s)));
        setIfExist(jn, COMMENT_MODIFICATION_DATE_FIELD,
                s -> comment.setModificationDate(s == null ? null : Instant.parse(s)));
        if (jn.has(EXTERNAL_ENTITY_ID_FIELD) || jn.has(EXTERNAL_ENTITY_ORIGIN_FIELD) || jn.has(EXTERNAL_ENTITY)) {
            ExternalEntity externalEntity = (ExternalEntity) comment;
            setIfExist(jn, EXTERNAL_ENTITY_ID_FIELD, externalEntity::setEntityId);
            setIfExist(jn, EXTERNAL_ENTITY_ORIGIN_FIELD, externalEntity::setOrigin);
            setIfExist(jn, EXTERNAL_ENTITY, externalEntity::setEntity);
        }
        return comment;
    }

    protected static void setIfExist(JsonNode jn, String key, Consumer<String> consumer) {
        if (jn.has(key)) {
            consumer.accept(jn.get(key).textValue());
        }
    }
}
