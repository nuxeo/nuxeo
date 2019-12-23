/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.Calendar;

/**
 * Context available when a blob is updated in a blob provider.
 *
 * @since 11.1
 */
public class BlobUpdateContext {

    /** The blob key, without blob provider prefix. */
    public final String key;

    public UpdateRetainUntil updateRetainUntil;

    public UpdateLegalHold updateLegalHold;

    public BlobUpdateContext(String key) {
        this.key = key;
    }

    public BlobUpdateContext with(BlobUpdateContext other) {
        if (other.updateRetainUntil != null) {
            updateRetainUntil = other.updateRetainUntil;
        }
        if (other.updateLegalHold != null) {
            updateLegalHold = other.updateLegalHold;
        }
        return this;
    }

    public BlobUpdateContext withUpdateRetainUntil(Calendar retainUntil) {
        updateRetainUntil = new UpdateRetainUntil(retainUntil);
        return this;
    }

    public BlobUpdateContext withUpdateLegalHold(boolean hold) {
        updateLegalHold = new UpdateLegalHold(hold);
        return this;
    }

    public static class UpdateRetainUntil {

        public final Calendar retainUntil;

        public UpdateRetainUntil(Calendar retainUntil) {
            this.retainUntil = retainUntil;
        }

    }

    public static class UpdateLegalHold {

        public final boolean hold;

        public UpdateLegalHold(boolean hold) {
            this.hold = hold;
        }

    }

}
