/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;

import java.util.function.Supplier;

import org.nuxeo.ecm.core.api.Lock;

import com.marklogic.client.document.DocumentMetadataPatchBuilder.PatchHandle;
import com.marklogic.client.document.DocumentPatchBuilder;
import com.marklogic.client.document.DocumentPatchBuilder.Position;

/**
 * Builder to update a document for lock features.
 *
 * @since 8.3
 */
class MarkLogicLockUpdateBuilder {

    private final Supplier<DocumentPatchBuilder> supplier;

    public MarkLogicLockUpdateBuilder(Supplier<DocumentPatchBuilder> supplier) {
        this.supplier = supplier;
    }

    public PatchHandle delete() {
        DocumentPatchBuilder patchBuilder = supplier.get();
        // Build patch
        String documentPath = MarkLogicHelper.DOCUMENT_ROOT_PATH + '/';
        patchBuilder.delete(documentPath + MarkLogicHelper.serializeKey(KEY_LOCK_OWNER));
        patchBuilder.delete(documentPath + MarkLogicHelper.serializeKey(KEY_LOCK_CREATED));
        return patchBuilder.build();
    }

    public PatchHandle set(Lock lock) {
        DocumentPatchBuilder patchBuilder = supplier.get();
        // Build patch
        patchBuilder.insertFragment(MarkLogicHelper.DOCUMENT_ROOT_PATH, Position.LAST_CHILD,
                MarkLogicStateSerializer.serialize(KEY_LOCK_OWNER, lock.getOwner()).get().asXML());
        patchBuilder.insertFragment(MarkLogicHelper.DOCUMENT_ROOT_PATH, Position.LAST_CHILD,
                MarkLogicStateSerializer.serialize(KEY_LOCK_CREATED, lock.getCreated()).get().asXML());
        return patchBuilder.build();
    }
}
