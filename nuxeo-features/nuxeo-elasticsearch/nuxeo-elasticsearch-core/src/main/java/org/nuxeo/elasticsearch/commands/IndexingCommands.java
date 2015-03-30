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
 *     Thierry Delprat
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.commands.IndexingCommand.Type;

/*
 * This class holds a list of indexing commands and manages de-duplication
 */
public class IndexingCommands {

    protected final List<IndexingCommand> commands = new ArrayList<>();

    protected final Set<Type> commandTypes = new HashSet<>();

    protected DocumentModel targetDocument;

    protected static final Log log = LogFactory.getLog(IndexingCommands.class);

    protected IndexingCommands() {
        //
    }

    public IndexingCommands(DocumentModel targetDocument) {
        this.targetDocument = targetDocument;
    }

    public void add(Type type, boolean sync, boolean recurse) {
        IndexingCommand cmd = new IndexingCommand(targetDocument, type, sync, recurse);
        add(cmd);
    }

    protected IndexingCommand find(Type command) {
        for (IndexingCommand cmd : commands) {
            if (cmd.type == command) {
                return cmd;
            }
        }
        return null;
    }

    protected void add(IndexingCommand command) {
        if (command == null) {
            return;
        }
        if (commandTypes.contains(command.type)) {
            IndexingCommand existing = find(command.type);
            if (existing.merge(command)) {
                return;
            }
        } else if (commandTypes.contains(Type.INSERT)) {
            if (command.type == Type.DELETE) {
                // index and delete in the same tx
                clear();
            } else if (command.isSync()) {
                // switch to sync if possible
                find(Type.INSERT).makeSync();
            }
            // we already have an index command, don't care about the new command
            return;
        }
        if (command.type == Type.DELETE) {
            // no need to keep event before delete.
            clear();
        }
        commands.add(command);
        commandTypes.add(command.type);
    }

    protected void clear() {
        commands.clear();
        commandTypes.clear();
    }

    public DocumentModel getTargetDocument() {
        return targetDocument;
    }

    public boolean contains(Type command) {
        return commandTypes.contains(command);
    }

    public List<IndexingCommand> getCommands() {
        return commands;
    }

}
