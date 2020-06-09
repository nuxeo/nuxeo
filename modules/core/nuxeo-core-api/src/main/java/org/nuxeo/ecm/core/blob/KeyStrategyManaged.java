/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.blob;

/**
 * Represents trusted managed blob key computation with a fallback {@link KeyStrategy}
 *
 * @since 11.2
 */
public class KeyStrategyManaged implements KeyStrategy {

    protected final KeyStrategy strategy;

    public KeyStrategyManaged(KeyStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean useDeDuplication() {
        return false;
    }

    @Override
    public String getDigestFromKey(String key) {
        return null;
    }

    @Override
    public BlobWriteContext getBlobWriteContext(BlobContext blobContext)  {
        if (blobContext.blob instanceof ManagedBlob) {
            String key = ((ManagedBlob) blobContext.blob).getKey();
            return new BlobWriteContext(blobContext, null, () -> key, this);
        } else {
            return strategy.getBlobWriteContext(blobContext);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KeyStrategyManaged)) {
            return false;
        }
        return strategy.equals(((KeyStrategyManaged) obj).strategy);
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
