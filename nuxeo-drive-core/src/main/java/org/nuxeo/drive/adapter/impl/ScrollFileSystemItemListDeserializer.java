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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * {@link JsonDeserializer} for a {@link ScrollFileSystemItemList}.
 *
 * @since 8.3
 */
public class ScrollFileSystemItemListDeserializer extends JsonDeserializer<ScrollFileSystemItemList> {

    @Override
    public ScrollFileSystemItemList deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        JsonNode rootNode = jp.readValueAsTree();
        String scrollId = ((TextNode) rootNode.get("scrollId")).textValue();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode fileSystemItemNodes = (ArrayNode) rootNode.get("fileSystemItems");
        List<FileSystemItem> fileSystemItems = new ArrayList<>(fileSystemItemNodes.size());
        for (JsonNode fileSystemItemNode : fileSystemItemNodes) {
            boolean folderish = ((BooleanNode) fileSystemItemNode.get("folder")).booleanValue();
            if (folderish) {
                fileSystemItems.add(readValue(mapper, fileSystemItemNode, DocumentBackedFolderItem.class));
            } else {
                fileSystemItems.add(readValue(mapper, fileSystemItemNode, DocumentBackedFileItem.class));
            }
        }
        return new ScrollFileSystemItemListImpl(scrollId, fileSystemItems);
    }

    protected <T> T readValue(ObjectMapper mapper, JsonNode node, Class<T> klass) throws IOException {
        try (JsonParser tokens = mapper.treeAsTokens(node)) {
            return mapper.readValue(tokens, klass);
        }
    }

}
