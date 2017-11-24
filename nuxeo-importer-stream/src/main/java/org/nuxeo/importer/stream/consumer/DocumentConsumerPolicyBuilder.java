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

import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicyBuilder;

/**
 * @since 9.2
 */
public class DocumentConsumerPolicyBuilder extends ConsumerPolicyBuilder {

    protected boolean blockIndexing = false;

    protected boolean bulkMode = false;

    protected boolean blockAsyncListeners = false;

    protected boolean blockPostCommitListeners = false;

    protected boolean blockDefaultSyncListener = false;

    public DocumentConsumerPolicyBuilder blockIndexing(boolean value) {
        this.blockIndexing = value;
        return this;
    }

    public DocumentConsumerPolicyBuilder useBulkMode(boolean value) {
        this.bulkMode = value;
        return this;
    }

    public DocumentConsumerPolicyBuilder blockAsyncListeners(boolean value) {
        this.blockAsyncListeners = value;
        return this;
    }

    public DocumentConsumerPolicyBuilder blockPostCommitListeners(boolean value) {
        this.blockPostCommitListeners = value;
        return this;
    }

    public DocumentConsumerPolicyBuilder blockDefaultSyncListener(boolean value) {
        this.blockDefaultSyncListener = value;
        return this;
    }

    public DocumentConsumerPolicy build() {
        return new DocumentConsumerPolicy(this);
    }

}
