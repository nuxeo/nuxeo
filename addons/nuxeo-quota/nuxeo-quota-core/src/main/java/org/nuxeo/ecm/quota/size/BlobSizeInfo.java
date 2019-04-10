/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */

package org.nuxeo.ecm.quota.size;

/**
 * helper class to transmis info about Blob changes during a transaction
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.6
 */
public class BlobSizeInfo {

    protected long blobSize = 0;

    protected long blobSizeDelta = 0;

    public long getBlobSize() {
        return blobSize;
    }

    public long getBlobSizeDelta() {
        return blobSizeDelta;
    }

    @Override
    public String toString() {
        return "total : " + blobSize + "; delta:" + blobSizeDelta;
    }
}
