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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer;

import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.lib.stream.pattern.consumer.Consumer;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerFactory;

/**
 * @since 9.1
 */
public class BlobMessageConsumerFactory implements ConsumerFactory<BlobMessage> {
    protected final String blobProviderName;

    protected final BlobInfoWriter blobInfoWriter;

    /**
     * Blob Consumer factory requires a blob providerName that is present in Nuxeo instance running the consumer. The
     * writer is used to store the blob information.
     */
    public BlobMessageConsumerFactory(String blobProviderName, BlobInfoWriter blobInfoWriter) {
        this.blobProviderName = blobProviderName;
        this.blobInfoWriter = blobInfoWriter;
    }

    @Override
    public Consumer<BlobMessage> createConsumer(String consumerId) {
        return new BlobMessageConsumer(consumerId, blobProviderName, blobInfoWriter);
    }
}
