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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.TextNode;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.ScrollFileSystemItemList;

/**
 * {@link JsonDeserializer} for a {@link ScrollFileSystemItemList}.
 *
 * @since 8.3
 */
public class ScrollFileSystemItemListDeserializer extends JsonDeserializer<ScrollFileSystemItemList> {

    @Override
    public ScrollFileSystemItemList deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        JsonNode rootNode = jp.readValueAsTree();
        String scrollId = ((TextNode) rootNode.get("scrollId")).getTextValue();
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode fileSystemItemNodes = (ArrayNode) rootNode.get("fileSystemItems");
        List<FileSystemItem> fileSystemItems = new ArrayList<>(fileSystemItemNodes.size());
        for (JsonNode fileSystemItemNode : fileSystemItemNodes) {
            boolean folderish = ((BooleanNode) fileSystemItemNode.get("folder")).getBooleanValue();
            if (folderish) {
                fileSystemItems.add(mapper.readValue(fileSystemItemNode, DocumentBackedFolderItem.class));
            } else {
                fileSystemItems.add(mapper.readValue(fileSystemItemNode, DocumentBackedFileItem.class));
            }
        }
        return new ScrollFileSystemItemListImpl(scrollId, fileSystemItems);
    }
}
