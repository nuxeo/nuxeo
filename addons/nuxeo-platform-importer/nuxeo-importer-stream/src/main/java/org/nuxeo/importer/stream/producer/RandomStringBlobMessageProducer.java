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

import java.util.concurrent.ThreadLocalRandom;

import org.nuxeo.ecm.platform.importer.random.HunspellDictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.RandomTextGenerator;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;

/**
 * Build random StringBlob message.
 *
 * @since 9.1
 */
public class RandomStringBlobMessageProducer extends AbstractProducer<BlobMessage> {
    protected static final String DEFAULT_MIME_TYPE = "plain/text";

    protected final long nbBlobs;

    protected final int averageSizeKB;

    protected final ThreadLocalRandom rand;

    protected final String marker;

    protected long count = 0;

    protected static RandomTextGenerator gen;

    protected final String mimetype;

    public RandomStringBlobMessageProducer(int producerId, long nbBlobs, String lang, int averageSizeKB,
            String marker) {
        super(producerId);
        this.nbBlobs = nbBlobs;
        this.averageSizeKB = averageSizeKB;
        this.mimetype = DEFAULT_MIME_TYPE;
        if (marker != null) {
            this.marker = marker.trim() + " ";
        } else {
            this.marker = "";
        }
        synchronized (RandomDocumentMessageProducer.class) {
            if (gen == null) {
                gen = new RandomTextGenerator(new HunspellDictionaryHolder(lang));
                gen.prefilCache();
            }
        }
        rand = ThreadLocalRandom.current();
    }

    @Override
    public int getPartition(BlobMessage message, int partitions) {
        return ((int) count) % partitions;
    }

    @Override
    public boolean hasNext() {
        return count < nbBlobs;
    }

    @Override
    public BlobMessage next() {
        String filename = generateFilename();
        String content = generateContent();
        BlobMessage ret = new BlobMessage.StringMessageBuilder(content).setFilename(filename)
                                                                       .setMimetype(mimetype)
                                                                       .build();
        count++;
        return ret;
    }

    protected String generateFilename() {
        return gen.getRandomTitle(rand.nextInt(4) + 1).trim().replaceAll("\\W+", "-").toLowerCase() + ".txt";
    }

    protected String generateContent() {
        return marker + gen.getRandomText(averageSizeKB);
    }

}
