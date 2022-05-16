/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.core;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nuxeo.runtime.pubsub.SerializableMessage;

/**
 * @since 2021.11
 */
public class ReindexingMessage implements SerializableMessage {
    private static final long serialVersionUID = 20220516L;

    public final String repository;

    public final String indexName;

    public final String secondWriteIndexName;

    public final ReindexingState state;

    protected static final String SEP = "/";

    protected static final String NUL = "null";

    /**
     * @param repository being re-indexed
     * @param indexName the name of the search index
     * @param secondIndexName the name of the second write index
     * @param state the state of the reindexing
     */
    public ReindexingMessage(String repository, String indexName, String secondIndexName, ReindexingState state) {
        Objects.requireNonNull(state);
        this.repository = repository;
        this.indexName = indexName;
        this.secondWriteIndexName = secondIndexName;
        this.state = state;
    }

    @Override
    public void serialize(OutputStream out) throws IOException {
        String string = repository + SEP + indexName + SEP + secondWriteIndexName + SEP + state;
        IOUtils.write(string, out, UTF_8);
    }

    public static ReindexingMessage deserialize(InputStream in) throws IOException {
        String string = IOUtils.toString(in, UTF_8);
        String[] parts = string.split(SEP, 4);
        if (parts.length != 4) {
            throw new IOException("Invalid reindexing message: " + string);
        }
        String repo = valueOf(parts[0]);
        String index = valueOf(parts[1]);
        String writeIndex = valueOf(parts[2]);
        ReindexingState state = ReindexingState.valueOf(parts[3]);
        return new ReindexingMessage(repo, index, writeIndex, state);
    }

    protected static String valueOf(String string) {
        return NUL.equals(string) ? null : string;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + repository + ", " + indexName + ", " + secondWriteIndexName
                + ", " + state + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReindexingMessage message = (ReindexingMessage) o;
        return new EqualsBuilder().append(repository, message.repository)
                                  .append(indexName, message.indexName)
                                  .append(secondWriteIndexName, message.secondWriteIndexName)
                                  .append(state, message.state)
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(repository)
                                          .append(indexName)
                                          .append(secondWriteIndexName)
                                          .append(state)
                                          .toHashCode();
    }
}
