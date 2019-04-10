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
package org.nuxeo.importer.stream.producer;

import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;

/**
 * @since 9.1
 */
public class RandomStringBlobMessageProducerFactory implements ProducerFactory<BlobMessage> {
    protected final long nbBlobs;

    protected final String lang;

    protected final int averageSizeKB;

    protected final String marker;

    /**
     * Produce messages with a random blob content
     */
    public RandomStringBlobMessageProducerFactory(long nbBlobs, String lang, int averageSizeKB, String marker) {
        this.lang = lang;
        this.nbBlobs = nbBlobs;
        this.averageSizeKB = averageSizeKB;
        this.marker = marker;
    }

    @Override
    public ProducerIterator<BlobMessage> createProducer(int producerId) {
        return new RandomStringBlobMessageProducer(producerId, nbBlobs, lang, averageSizeKB, marker);
    }
}
