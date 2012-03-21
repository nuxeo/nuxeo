/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.connect.update.standalone.task.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
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
        commands = new ArrayList<Command>();
    }

    public CompositeCommand() {
        super(ID);
        commands = new ArrayList<Command>();
    }

    @Override
    public void writeTo(XmlWriter writer) {
        for (Command command : commands) {
            command.writeTo(writer);
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        CompositeCommand rollbackCommand = new CompositeCommand();
        for (Command command : commands) {
            rollbackCommand.addCommand(command.run(task, prefs));
        }
        return rollbackCommand;
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        for (Command command : commands) {
            command.validate(task, status);
        }
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        throw new UnsupportedOperationException(
                "Composite command is for internal use only.");
    }

    public void addCommand(Command command) {
        if (command != null) {
            commands.add(command);
        }
    }

}
