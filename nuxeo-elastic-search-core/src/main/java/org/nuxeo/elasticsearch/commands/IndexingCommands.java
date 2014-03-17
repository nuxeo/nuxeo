/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

    protected List<IndexingCommand> commands = new ArrayList<>();

    protected List<String> commandNames = new ArrayList<>();

    protected DocumentModel targetDocument;

    protected static final Log log = LogFactory.getLog(IndexingCommands.class);

    protected IndexingCommands() {
        //
    }

    public IndexingCommands(String command, DocumentModel targetDocument,
            boolean sync, boolean recurse) {
        this.targetDocument = targetDocument;
        add(command, sync, recurse);
    }

    public IndexingCommands(DocumentModel targetDocument) {
        this.targetDocument = targetDocument;
    }

    public IndexingCommand add(String command, boolean sync, boolean recurse) {
        IndexingCommand cmd = new IndexingCommand(targetDocument, command,
                sync, recurse);
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

        // skip duplicates
        if (commandNames.contains(command.name)) {
            find(command.name).update(command);
            return null;
        }

        // index creation supersedes
        if (commandNames.contains(IndexingCommand.INDEX)) {
            if (command.name.equals(IndexingCommand.DELETE)) {
                clear();
            } else if (command.isSync()) {
                find(IndexingCommand.INDEX).sync = true;
            }
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

    public List<IndexingCommand> getMergedCommands() {
        return commands;
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

}
