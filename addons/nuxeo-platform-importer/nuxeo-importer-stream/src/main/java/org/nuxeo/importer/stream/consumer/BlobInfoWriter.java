/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer;

import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.importer.stream.message.DocumentMessage;

/**
 * Save blob information for a document.
 *
 * @since 9.3
 */
public interface BlobInfoWriter extends AutoCloseable {

    void save(DocumentMessage.Builder builder, BlobInfo info);

    @Override
    void close();
}
