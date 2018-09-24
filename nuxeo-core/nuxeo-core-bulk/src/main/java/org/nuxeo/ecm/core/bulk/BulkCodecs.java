/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.core.bulk;

import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkCounter;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * Helper class for codecs.
 *
 * @since 10.3
 */
public class BulkCodecs {

    public static final String DEFAULT_CODEC = "avro";

    private BulkCodecs() {
        // utility class
    }

    public static Codec<BulkCommand> getCommandCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, BulkCommand.class);
    }

    public static Codec<BulkStatus> getStatusCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, BulkStatus.class);
    }

    public static Codec<BulkCounter> getCounterCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, BulkCounter.class);
    }

    public static Codec<BulkBucket> getBucketCodec() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, BulkBucket.class);
    }

}
