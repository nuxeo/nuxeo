/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.connect.update.task.standalone.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 * Command embedding multiple commands. For internal use.
 *
 * @since 5.5
 */
public class CompositeCommand extends AbstractCommand {

    public static final String ID = "composite";

    protected final List<Command> commands;

    protected CompositeCommand(String id) {
        super(id);
        commands = new ArrayList<>();
    }

    public CompositeCommand() {
        super(ID);
        commands = new ArrayList<>();
    }

    @Override
    public void writeTo(XmlWriter writer) {
        for (Command command : commands) {
            command.writeTo(writer);
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        CompositeCommand rollbackCommand = new CompositeCommand();
        for (Command command : commands) {
            rollbackCommand.addCommand(command.run(task, prefs));
        }
        return rollbackCommand;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status) throws PackageException {
        for (Command command : commands) {
            command.validate(task, status);
        }
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        throw new UnsupportedOperationException("Composite command is for internal use only.");
    }

    public void addCommand(Command command) {
        if (command != null) {
            commands.add(command);
        }
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    /**
     * @since 9.3
     */
    public CompositeCommand combine(CompositeCommand cc) {
        commands.addAll(cc.commands);
        return this;
    }

}
