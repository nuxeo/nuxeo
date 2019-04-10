/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.drive.service.impl;

import java.io.IOException;
import java.util.List;

import org.nuxeo.drive.service.FileSystemItemChange;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * {@link JsonDeserializer} for a {@link List<FileSystemItemChange>}.
 *
 * @author Antoine Taillefer
 */
public class FileSystemItemChangeListDeserializer extends JsonDeserializer<List<FileSystemItemChange>> {

    static final TypeReference<List<FileSystemItemChangeImpl>> LIST_TYPE = new TypeReference<List<FileSystemItemChangeImpl>>() {
    };

    @Override
    public List<FileSystemItemChange> deserialize(JsonParser jp, DeserializationContext dc) throws IOException {
        return jp.readValueAs(LIST_TYPE);
    }

}
