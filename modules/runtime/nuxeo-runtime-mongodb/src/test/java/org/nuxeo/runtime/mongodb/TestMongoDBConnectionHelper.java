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
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime.mongodb;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

/**
 * @since 11.4
 */
public class TestMongoDBConnectionHelper {

    // NXP-29111
    @Test
    public void testPopulateProperties() {
        var settingsBuilder = MongoClientSettings.builder().applicationName("properties-tester");
        var config = new MongoDBConnectionConfig();
        config.properties = Map.of("readPreference", "primary", "readConcern", "majority", "writeConcern",
                "acknowledged");

        MongoDBConnectionHelper.populateProperties(config, settingsBuilder);
        MongoClientSettings settings = settingsBuilder.build();

        assertEquals(ReadPreference.primary(), settings.getReadPreference());
        assertEquals(ReadConcern.MAJORITY, settings.getReadConcern());
        assertEquals(WriteConcern.ACKNOWLEDGED, settings.getWriteConcern());
    }
}
