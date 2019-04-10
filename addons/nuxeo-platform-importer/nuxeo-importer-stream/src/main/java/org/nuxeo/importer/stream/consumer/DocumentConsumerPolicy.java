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

import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;

/**
 * @since 9.2
 */
public class DocumentConsumerPolicy extends ConsumerPolicy {
    protected boolean blockIndexing;

    protected boolean bulkMode;

    protected boolean blockAsyncListeners;

    protected boolean blockPostCommitListeners;

    protected boolean blockDefaultSyncListeners;

    public DocumentConsumerPolicy(DocumentConsumerPolicyBuilder builder) {
        super(builder);
        this.blockIndexing = builder.blockIndexing;
        this.blockAsyncListeners = builder.blockAsyncListeners;
        this.blockDefaultSyncListeners = builder.blockDefaultSyncListener;
        this.blockPostCommitListeners = builder.blockPostCommitListeners;
        this.bulkMode = builder.bulkMode;
    }

    public boolean bulkMode() {
        return bulkMode;
    }

    public boolean blockAsyncListeners() {
        return blockAsyncListeners;
    }

    public boolean blockPostCommitListeners() {
        return blockPostCommitListeners;
    }

    public boolean blockDefaultSyncListeners() {
        return blockDefaultSyncListeners;
    }

    public boolean blockIndexing() {
        return blockIndexing;
    }

    public static DocumentConsumerPolicyBuilder builder() {
        return new DocumentConsumerPolicyBuilder();
    }

    @Override
    public String toString() {
        return "DocumentConsumerPolicy{" + "blockIndexing=" + blockIndexing + ", bulkMode=" + bulkMode
                + ", blockAsyncListeners=" + blockAsyncListeners + ", blockPostCommitListeners="
                + blockPostCommitListeners + ", blockDefaultSyncListeners=" + blockDefaultSyncListeners + ", "
                + super.toString() + '}';
    }
}
