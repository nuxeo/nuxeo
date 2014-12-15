/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.type.TypeReference;
import org.nuxeo.drive.service.FileSystemItemChange;

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
