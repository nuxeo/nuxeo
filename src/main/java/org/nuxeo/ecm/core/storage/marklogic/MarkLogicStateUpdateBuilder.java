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

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.dom4j.Element;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;

import com.marklogic.client.document.DocumentMetadataPatchBuilder.Call;
import com.marklogic.client.document.DocumentMetadataPatchBuilder.Cardinality;
import com.marklogic.client.document.DocumentMetadataPatchBuilder.PatchHandle;
import com.marklogic.client.document.DocumentPatchBuilder;
import com.marklogic.client.document.DocumentPatchBuilder.Position;

/**
 * Builder to convert a {@link StateDiff} into a {@link PatchHandle}.
 *
 * @since 8.3
 */
class MarkLogicStateUpdateBuilder implements Function<StateDiff, PatchHandle> {

    private final Supplier<DocumentPatchBuilder> supplier;

    public MarkLogicStateUpdateBuilder(Supplier<DocumentPatchBuilder> supplier) {
        this.supplier = supplier;
    }

    @Override
    public PatchHandle apply(StateDiff diff) {
        DocumentPatchBuilder patchBuilder = supplier.get();
        // Build patch
        fillPatch(patchBuilder, MarkLogicHelper.DOCUMENT_ROOT_PATH, diff);
        return patchBuilder.build();
    }

    private void fillPatch(DocumentPatchBuilder patchBuilder, String path, StateDiff diff) {
        for (Entry<String, Serializable> entry : diff.entrySet()) {
            String subPath = path + "/" + MarkLogicHelper.serializeKey(entry.getKey());
            Serializable value = entry.getValue();
            if (value instanceof StateDiff) {
                fillPatch(patchBuilder, subPath, (StateDiff) value);
            } else if (value instanceof ListDiff) {
                fillPatch(patchBuilder, subPath, (ListDiff) value);
            } else if (value instanceof Delta) {
                Call call = patchBuilder.call().add(((Delta) value).getDeltaValue());
                patchBuilder.replaceApply(subPath, Cardinality.ONE, call);
            } else {
                Optional<Element> fragment = MarkLogicStateSerializer.serialize(entry.getKey(), value);
                if (fragment.isPresent()) {
                    patchBuilder.replaceInsertFragment(subPath, path, Position.LAST_CHILD, fragment.get().asXML());
                } else {
                    patchBuilder.delete(subPath);
                }
            }
        }
    }

    private void fillPatch(DocumentPatchBuilder patchBuilder, String path, ListDiff listDiff) {
        if (listDiff.diff != null) {
            int i = 1;
            for (Object value : listDiff.diff) {
                String subPath = path + '/' + MarkLogicHelper.ARRAY_ITEM_KEY + '[' + i + ']';
                if (value instanceof StateDiff) {
                    fillPatch(patchBuilder, subPath, (StateDiff) value);
                } else if (value != State.NOP) {
                    Optional<Element> fragment = MarkLogicStateSerializer.serialize(MarkLogicHelper.ARRAY_ITEM_KEY,
                            value);
                    if (fragment.isPresent()) {
                        patchBuilder.replaceFragment(subPath, Cardinality.ONE, fragment.get().asXML());
                    } else {
                        patchBuilder.delete(subPath);
                    }
                }
                i++;
            }
        }
        if (listDiff.rpush != null) {
            for (Object value : listDiff.rpush) {
                Element fragment = MarkLogicStateSerializer.serialize(MarkLogicHelper.ARRAY_ITEM_KEY, value).get();
                patchBuilder.insertFragment(path, Position.LAST_CHILD, fragment.asXML());
            }
        }
    }
}
