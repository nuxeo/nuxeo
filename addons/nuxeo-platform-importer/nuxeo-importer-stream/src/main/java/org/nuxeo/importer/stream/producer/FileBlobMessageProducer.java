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
package org.nuxeo.importer.stream.producer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.lib.stream.pattern.producer.AbstractProducer;

/**
 * Use a listing file to generate BlobMessage.
 *
 * @since 9.1
 */
public class FileBlobMessageProducer extends AbstractProducer<BlobMessage> {
    private static final Log log = LogFactory.getLog(FileBlobMessageProducer.class);

    protected final File listFile;

    protected final String basePath;

    protected long count = 0;

    protected final long nbBlobs;

    protected Iterator<String> fileIterator;

    protected Stream<String> lines;

    public FileBlobMessageProducer(int producerId, File listFile, String basePath, long nbBlobs) {
        super(producerId);
        this.listFile = listFile;
        this.nbBlobs = nbBlobs;
        this.basePath = StringUtils.defaultString(basePath);
        log.info("Producer using file list: " + listFile.getAbsolutePath());
        getFileIterator();

    }

    protected void getFileIterator() {
        try {
            if (lines != null) {
                lines.close();
            }
            lines = Files.lines(listFile.toPath());
            fileIterator = lines.iterator();
        } catch (IOException e) {
            String msg = "Failed to read file: " + listFile.getAbsolutePath();
            log.error(msg, e);
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int getPartition(BlobMessage message, int partitions) {
        return ((int) count) % partitions;
    }

    @Override
    public void close() throws Exception {
        super.close();
        fileIterator = null;
        if (lines != null) {
            lines.close();
        }
        lines = null;
    }

    @Override
    public boolean hasNext() {
        if (nbBlobs > 0 && count >= nbBlobs) {
            return false;
        }
        if (!fileIterator.hasNext()) {
            if (nbBlobs == 0) {
                return false;
            }
            // loop until we get the nb blobs
            getFileIterator();
        }
        return true;
    }

    @Override
    public BlobMessage next() {
        String filePath = fileIterator.next();
        count += 1;
        return new BlobMessage.FileMessageBuilder(new File(basePath, filePath).toString()).build();
    }
}
