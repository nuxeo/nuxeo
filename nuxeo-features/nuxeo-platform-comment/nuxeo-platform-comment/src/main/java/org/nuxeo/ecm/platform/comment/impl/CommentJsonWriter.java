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
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ANCESTOR_IDS_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_AUTHOR_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_CREATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_LAST_REPLY_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_MODIFICATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_NUMBER_OF_REPLIES_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PERMISSIONS_FIELD;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_FIELD;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_FIELD;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class CommentJsonWriter extends ExtensibleEntityJsonWriter<Comment> {

    public static final String FETCH_REPLIES_SUMMARY = "repliesSummary";

    @Inject
    protected CommentManager commentManager;

    public CommentJsonWriter() {
        super(COMMENT_ENTITY_TYPE, Comment.class);
    }

    @Override
    protected void writeEntityBody(Comment entity, JsonGenerator jg) throws IOException {
        writeCommentEntity(entity, jg);
        CoreSession session = ctx.getSession(null).getSession();
        NuxeoPrincipal principal = session.getPrincipal();
        PermissionProvider permissionProvider = Framework.getService(PermissionProvider.class);

        // Write permissions of current user on the annotation,
        // which are the ones granted on the commented document
        Collection<String> permissions = CoreInstance.doPrivileged(session, s -> {
            if (entity.getId() == null) {
                return Collections.emptyList();
            }
            DocumentRef ancestorRef = Framework.getService(CommentManager.class)
                                               .getTopLevelCommentAncestor(s, new IdRef(entity.getId()));
            return s.filterGrantedPermissions(principal, ancestorRef,
                    Arrays.asList(permissionProvider.getPermissions()));
        });
        jg.writeArrayFieldStart(COMMENT_PERMISSIONS_FIELD);
        for (String permission : permissions) {
            jg.writeString(permission);
        }
        jg.writeEndArray();
        boolean includeRepliesSummary = ctx.getFetched(COMMENT_ENTITY_TYPE).contains(FETCH_REPLIES_SUMMARY);
        if (includeRepliesSummary) {
            writeRepliesSummary(session, entity, jg);
        }
    }

    protected static void writeCommentEntity(Comment entity, JsonGenerator jg) throws IOException {
        jg.writeStringField(COMMENT_ID_FIELD, entity.getId());
        jg.writeStringField(COMMENT_PARENT_ID_FIELD, entity.getParentId());
        jg.writeArrayFieldStart(COMMENT_ANCESTOR_IDS_FIELD);
        for (String ancestorId : entity.getAncestorIds()) {
            jg.writeString(ancestorId);
        }
        jg.writeEndArray();
        jg.writeStringField(COMMENT_AUTHOR_FIELD, entity.getAuthor());
        jg.writeStringField(COMMENT_TEXT_FIELD, entity.getText());

        String creationDate = entity.getCreationDate() != null ? entity.getCreationDate().toString() : null;
        jg.writeStringField(COMMENT_CREATION_DATE_FIELD, creationDate);
        String modificationDate = entity.getModificationDate() != null ? entity.getModificationDate().toString() : null;
        jg.writeStringField(COMMENT_MODIFICATION_DATE_FIELD, modificationDate);

        if (entity instanceof ExternalEntity) {
            jg.writeStringField(EXTERNAL_ENTITY_ID_FIELD, ((ExternalEntity) entity).getEntityId());
            jg.writeStringField(EXTERNAL_ENTITY_ORIGIN_FIELD, ((ExternalEntity) entity).getOrigin());
            jg.writeStringField(EXTERNAL_ENTITY, ((ExternalEntity) entity).getEntity());
        }
    }

    protected void writeRepliesSummary(CoreSession session, Comment entity, JsonGenerator jg) throws IOException {
        PartialList<Comment> comments = commentManager.getComments(session, entity.getId(), 1L, 0L, false);
        jg.writeNumberField(COMMENT_NUMBER_OF_REPLIES_FIELD, comments.totalSize());
        if (!comments.isEmpty()) {
            jg.writeStringField(COMMENT_LAST_REPLY_DATE_FIELD, comments.get(0).getCreationDate().toString());
        }
    }
}
