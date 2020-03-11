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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_PERMISSIONS_FIELD;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_FIELD;
import static org.nuxeo.ecm.platform.comment.impl.CommentJsonWriter.writeCommentEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationConstants;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class AnnotationJsonWriter extends ExtensibleEntityJsonWriter<Annotation> {

    /**
     * @deprecated since 11.1, use {@link AnnotationConstants#ANNOTATION_ENTITY_TYPE} instead.
     */
    @Deprecated(since = "11.1")
    public static final String ENTITY_TYPE = "annotation";

    public AnnotationJsonWriter() {
        super(ANNOTATION_ENTITY_TYPE, Annotation.class);
    }

    @Override
    protected void writeEntityBody(Annotation entity, JsonGenerator jg) throws IOException {
        writeCommentEntity(entity, jg);
        jg.writeStringField(ANNOTATION_XPATH_FIELD, entity.getXpath());
        // Write permissions of current user on the annotation,
        // which are the ones granted on the annotated document
        CoreSession session = ctx.getSession(null).getSession();
        NuxeoPrincipal principal = session.getPrincipal();
        PermissionProvider permissionProvider = Framework.getService(PermissionProvider.class);
        Collection<String> permissions = CoreInstance.doPrivileged(session, s -> {
            return s.filterGrantedPermissions(principal, new IdRef(entity.getParentId()),
                    Arrays.asList(permissionProvider.getPermissions()));
        });
        jg.writeArrayFieldStart(ANNOTATION_PERMISSIONS_FIELD);
        for (String permission : permissions) {
            jg.writeString(permission);
        }
        jg.writeEndArray();
    }
}
