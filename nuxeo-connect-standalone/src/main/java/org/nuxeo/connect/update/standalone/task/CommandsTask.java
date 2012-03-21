/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.standalone.task;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.standalone.task.commands.Command;
import org.nuxeo.connect.update.standalone.task.commands.Flush;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * A command based task.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class CommandsTask extends AbstractTask {

    protected final List<Command> commands;

    /**
     * The log is generated in the inverse order of commands to ensure last
     * command is rollbacked first.
     */
    protected final LinkedList<Command> log;

    protected CommandsTask() {
        commands = new ArrayList<Command>();
        log = new LinkedList<Command>();
    }

    /**
     * Get the commands file from where to load commands for this task.
     */
    protected abstract File getCommandsFile() throws PackageException;

    @Override
    public void initialize(LocalPackage pkg, boolean restart)
            throws PackageException {
        super.initialize(pkg, restart);
        loadCommands();
    }

    /**
     * Load the commands of this task given the user parameters. The parameter
     * map may be null.
     */
    protected void loadCommands() throws PackageException {
        try {
            String content = loadParametrizedFile(getCommandsFile(), env);
            StringReader reader = new StringReader(content);
            readLog(reader);
        } catch (IOException e) {
            throw new PackageException("Failed to load commands file", e);
        }
    }

    /**
     * Gets the commands to execute.
     */
    public List<Command> getCommands() {
        return commands;
    }

    /**
     * Gets the command log. These are the commands ran so far.
     */
    public List<Command> getCommandLog() {
        return log;
    }

    /**
     * Adds a command to this task.
     */
    public void addCommand(Command command) {
        commands.add(command);
    }

    /**
     * User parameters are not handled by default. You need to implement your
     * own task to o this.
     */
    protected void doRun(Map<String, String> params) throws PackageException {
        for (Command cmd : commands) {
            Command rollbackCmd = cmd.run(this, params);
            if (rollbackCmd != null) {
                if (rollbackCmd.isPostInstall()) {
                    log.add(rollbackCmd);
                } else {
                    log.addFirst(rollbackCmd);
                }
            }
        }
        // TODO: force a flush?
        try {
            Flush.flush();
        } catch (Exception e) {
            throw new PackageException("cache flushing failed", e);
        }
    }

    protected void doRollback() throws PackageException {
        while (!log.isEmpty()) {
            log.removeFirst().run(this, null);
        }
    }

    public void doValidate(ValidationStatus status) throws PackageException {
        // the target platform is not checked at install
        // check that commands can be run
        for (Command cmd : commands) {
            cmd.validate(this, status);
        }
    }

    public void writeLog(File file) throws PackageException {
        XmlWriter writer = new XmlWriter();
        writer.start("uninstall");
        writer.startContent();
        for (Command cmd : log) {
            cmd.writeTo(writer);
        }
        writer.end("uninstall");
        try {
            // replace all occurrences of the installation path with the
            // corresponding variable otherwise the uninstall will not work
            // after renaming the installation directory
            String content = parametrizePaths(writer.toString());
            content = content.replace(File.separator.concat(File.separator),
                    File.separator); // replace '//' by '/' is any
            FileUtils.writeFile(file, content);
        } catch (IOException e) {
            throw new PackageException("Failed to write commands", e);
        }
    }

    public String parametrizePaths(String content) {
        return content.replace(serverPathPrefix, "${" + ENV_SERVER_HOME + "}/");
    }

    public void readLog(Reader reader) throws PackageException {
        UpdateServiceImpl reg = (UpdateServiceImpl) Framework.getLocalService(PackageUpdateService.class);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(reader));
            Element root = document.getDocumentElement();
            Node node = root.getFirstChild();
            while (node != null) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String id = node.getNodeName();
                    Command cmd = reg.getCommand(id);
                    if (cmd == null) { // may be the name of an embedded class
                        try {
                            cmd = (Command) pkg.getData().loadClass(id).getConstructor().newInstance();
                        } catch (Throwable t) {
                            throw new PackageException("Unknown command: " + id);
                        }
                    }
                    cmd.initialize(element);
                    commands.add(cmd);
                }
                node = node.getNextSibling();
            }
        } catch (Exception e) {
            throw new PackageException("Failed to read commands", e);
        }
    }

}
