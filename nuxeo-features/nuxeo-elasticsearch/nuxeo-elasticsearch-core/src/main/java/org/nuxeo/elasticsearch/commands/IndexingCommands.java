/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo
 */
package org.nuxeo.elasticsearch.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/*
 * This class holds a list of indexing commands and manages de-duplication
 */
public class IndexingCommands {

    protected final List<IndexingCommand> commands = new ArrayList<>();

    protected final List<String> commandNames = new ArrayList<>();

    protected DocumentModel targetDocument;

    protected static final Log log = LogFactory.getLog(IndexingCommands.class);

    protected IndexingCommands() {
        //
    }

    public IndexingCommands(DocumentModel targetDocument) {
        this.targetDocument = targetDocument;
    }

    public IndexingCommand add(String command, boolean sync, boolean recurse) {
        IndexingCommand cmd;
        if (sync && recurse) {
            // split into 2 commands one sync and an async recurse
            cmd = new IndexingCommand(targetDocument, command,
                    true, false);
            add(cmd);
            cmd = new IndexingCommand(targetDocument, command,
                    false, true);

        } else {
            cmd = new IndexingCommand(targetDocument, command,
                    sync, recurse);
        }
        return add(cmd);
    }

    protected IndexingCommand find(String command) {
        for (IndexingCommand cmd : commands) {
            if (cmd.name.equals(command)) {
                return cmd;
            }
        }
        return null;
    }

    public IndexingCommand add(IndexingCommand command) {

        if (command == null) {
            return null;
        }

        if (commandNames.contains(command.name)) {
            IndexingCommand existing = find(command.name);
            if (existing.canBeMerged(command)) {
                existing.merge(command);
                return null;
            }
        } else if (commandNames.contains(IndexingCommand.INSERT)) {
            if (command.name.equals(IndexingCommand.DELETE)) {
                // index and delete in the same tx
                clear();
            } else if (command.isSync()) {
                // switch to sync if possible
                find(IndexingCommand.INSERT).makeSync();
            }
            // we already have an index command, don't care about the new command
            return null;
        }

        if (command.name.equals(IndexingCommand.DELETE)) {
            // no need to keep event before delete.
            clear();
        }

        commands.add(command);
        commandNames.add(command.name);
        return command;
    }

    protected void clear() {
        commands.clear();
        commandNames.clear();
    }

    public DocumentModel getTargetDocument() {
        return targetDocument;
    }

    public boolean contains(String command) {
        return commandNames.contains(command);
    }

    public String toJSON() throws IOException {
        StringWriter out = new StringWriter();
        JsonFactory factory = new JsonFactory();
        JsonGenerator jsonGen = factory.createJsonGenerator(out);
        jsonGen.writeStartArray();
        for (IndexingCommand cmd : commands) {
            cmd.toJSON(jsonGen);
        }
        jsonGen.writeEndArray();
        out.flush();
        jsonGen.close();
        return out.toString();
    }

    public static IndexingCommands fromJSON(CoreSession session, String json)
            throws ClientException {
        try {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jp = jsonFactory.createJsonParser(json);
            try {
                return fromJSON(session, jp);
            } finally {
                jp.close();
            }
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }
    }

    public static IndexingCommands fromJSON(CoreSession session, JsonParser jp)
            throws Exception {
        IndexingCommands cmds = new IndexingCommands();
        JsonToken token = jp.nextToken();
        if (token != JsonToken.START_ARRAY) {
            return null;
        }
        while (token != JsonToken.END_ARRAY) {
            IndexingCommand cmd = IndexingCommand.fromJSON(session, jp);
            if (cmd == null) {
                break;
            } else {
                cmds.add(cmd);
            }
        }
        return cmds;
    }

    public List<IndexingCommand> getCommands() {
        return commands;
    }

}

