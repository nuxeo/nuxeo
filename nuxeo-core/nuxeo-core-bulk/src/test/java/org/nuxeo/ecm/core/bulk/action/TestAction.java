/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.bulk.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.BulkCodecs;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.BulkServiceImpl;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkBucket;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestAction {

    @Test
    public void testUnknownCommand() {
        final String command = "unknownCommand";

        Record record1 = createRecord(command, Arrays.asList("id1", "id2"));

        // init the computation
        SetPropertiesAction.SetPropertyComputation comp = new SetPropertiesAction.SetPropertyComputation();
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        // submit a record with an unknown command id
        comp.processRecord(context, "i1", record1);

        // this stop the computation because it requires manual intervention to continue
        assertTrue(context.requireTerminate());
        assertEquals(0, context.getRecords("o1").size());
        assertFalse(context.requireCheckpoint());

        comp.destroy();
    }

    @Test
    public void testAbortedCommand() {
        final String commandId = "abortedCommand";
        Record record1 = createRecord(commandId, Arrays.asList("id1", "id2"));

        // init the computation
        SetPropertiesAction.SetPropertyComputation comp = new SetPropertiesAction.SetPropertyComputation();
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), Collections.emptyMap()));
        comp.init(context);

        abortStatus(commandId);

        comp.processRecord(context, "i1", record1);

        // the computation skip the record
        assertFalse(context.requireTerminate());
        assertTrue(context.requireCheckpoint());
        assertEquals(0, context.getRecords("o1").size());

        comp.destroy();
    }

    protected Record createRecord(String commandId, List<String> ids) {
        Codec<BulkBucket> codec = BulkCodecs.getBucketCodec();
        BulkBucket bucket = new BulkBucket(commandId, ids);
        return Record.of(commandId, codec.encode(bucket));
    }

    protected void abortStatus(String commandId) {
        BulkStatus status = new BulkStatus(commandId);
        status.setState(BulkStatus.State.ABORTED);
        BulkServiceImpl service = (BulkServiceImpl) Framework.getService(BulkService.class);
        service.setStatus(status);
    }
}
