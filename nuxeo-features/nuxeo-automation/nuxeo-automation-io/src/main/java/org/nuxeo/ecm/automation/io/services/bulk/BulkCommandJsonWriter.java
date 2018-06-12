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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.bulk;

import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_ENTITY_TYPE;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_OPERATION;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_QUERY;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_REPOSITORY;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_USERNAME;

import java.io.IOException;

import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.2
 */
public class BulkCommandJsonWriter extends ExtensibleEntityJsonWriter<BulkCommand> {

    public BulkCommandJsonWriter() {
        super(COMMAND_ENTITY_TYPE, BulkCommand.class);
    }

    @Override
    protected void writeEntityBody(BulkCommand command, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        // everything is mandatory
        jg.writeStringField(COMMAND_USERNAME, command.getUsername());
        jg.writeStringField(COMMAND_REPOSITORY, command.getRepository());
        jg.writeStringField(COMMAND_QUERY, command.getQuery());
        jg.writeStringField(COMMAND_OPERATION, command.getAction());
        jg.writeEndObject();
    }
}
