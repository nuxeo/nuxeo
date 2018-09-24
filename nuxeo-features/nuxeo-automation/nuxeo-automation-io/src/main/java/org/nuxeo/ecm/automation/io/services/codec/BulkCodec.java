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
 *     pierre
 */
package org.nuxeo.ecm.automation.io.services.codec;

import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_ENTITY_TYPE;

import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.io.BulkJsonReader;
import org.nuxeo.ecm.core.bulk.io.BulkJsonWriter;

/**
 * @since 10.2
 */
public class BulkCodec extends AbstractMarshallingRegistryCodec<BulkStatus> {

    public BulkCodec() {
        super(BulkStatus.class, BULK_ENTITY_TYPE, BulkJsonReader.class, BulkJsonWriter.class);
    }

}
