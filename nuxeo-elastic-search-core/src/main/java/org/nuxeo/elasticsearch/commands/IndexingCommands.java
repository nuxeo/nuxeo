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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/*
 * This class holds a list of indexing commands and manages de-duplication
 */
public class IndexingCommands {

    protected List<IndexingCommand> commands = new ArrayList<>();

    protected List<String> commandNames = new ArrayList<>();

    protected final DocumentModel targetDocument;

    public IndexingCommands(String command, DocumentModel targetDocument,
            boolean sync, boolean recurse) {
        this.targetDocument = targetDocument;
        add(command, sync, recurse);
    }

    public IndexingCommands(DocumentModel targetDocument) {
        this.targetDocument = targetDocument;
    }

    public void add(String command, boolean sync, boolean recurse) {
        add(new IndexingCommand(command, sync, recurse));
    }

    protected IndexingCommand find(String command) {
        for (IndexingCommand cmd : commands) {
            if (cmd.name.equals(command)) {
                return cmd;
            }
        }
        return null;
    }

    public void add(IndexingCommand command) {

        // skip duplicates
        if (commandNames.contains(command.name)) {
            find(command.name).update(command);
            return;
        }

        // index creation supersedes
        if (commandNames.contains(IndexingCommand.INDEX)) {
            if (command.name.equals(IndexingCommand.DELETE)) {
                clear();
            } else if (command.isSync()){
                find(IndexingCommand.INDEX).sync=true;
            }
            return;
        }

        if (command.name.equals(IndexingCommand.DELETE)) {
            // no need to keep event before delete.
            clear();
        }

        commands.add(command);
        commandNames.add(command.name);
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

}
