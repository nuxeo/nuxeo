/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.connect.update.task.standalone;

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
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.nuxeo.connect.update.LocalPackage;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A command based task.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class CommandsTask extends AbstractTask {

    protected final List<Command> commands;

    /**
     * The log is generated in the inverse order of commands to ensure last command is rollbacked first.
     */
    protected final LinkedList<Command> commandLog;

    public CommandsTask(PackageUpdateService pus) {
        super(pus);
        commands = new ArrayList<>();
        commandLog = new LinkedList<>();
    }

    /**
     * Get the commands file from where to load commands for this task.
     */
    protected abstract File getCommandsFile() throws PackageException;

    @SuppressWarnings("hiding")
    @Override
    public void initialize(LocalPackage pkg, boolean restart) throws PackageException {
        super.initialize(pkg, restart);
        loadCommands();
    }

    /**
     * Load the commands of this task given the user parameters. The parameter map may be null.
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
        return commandLog;
    }

    /**
     * Adds a command to this task.
     */
    public void addCommand(Command command) {
        commands.add(command);
    }

    /**
     * User parameters are not handled by default. You need to implement your own task to do this.
     */
    @Override
    protected void doRun(Map<String, String> params) throws PackageException {
        for (Command cmd : commands) {
            Command rollbackCmd = cmd.run(this, params);
            if (rollbackCmd != null) {
                if (rollbackCmd.isPostInstall()) {
                    commandLog.add(rollbackCmd);
                } else {
                    commandLog.addFirst(rollbackCmd);
                }
            }
        }
        // XXX: force a flush?
        flush();
    }

    /**
     * @throws PackageException
     * @since 5.6
     */
    protected abstract void flush() throws PackageException;

    @Override
    protected void doRollback() throws PackageException {
        while (!commandLog.isEmpty()) {
            commandLog.removeFirst().run(this, null);
        }
    }

    @Override
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
        for (Command cmd : commandLog) {
            cmd.writeTo(writer);
        }
        writer.end("uninstall");
        try {
            // replace all occurrences of the installation path with the corresponding variable otherwise the uninstall
            // will not work after renaming the installation directory
            String content = parametrizePaths(writer.toString());
            // replace '//' by '/' if any
            content = content.replace(File.separator.concat(File.separator), File.separator);
            FileUtils.writeStringToFile(file, content);
        } catch (IOException e) {
            throw new PackageException("Failed to write commands", e);
        }
    }

    public String parametrizePaths(String content) {
        return content.replace(serverPathPrefix, "${" + ENV_SERVER_HOME + "}/");
    }

    public void readLog(Reader reader) throws PackageException {
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
                    Command cmd = service.getCommand(id);
                    if (cmd == null) { // may be the name of an embedded class
                        try {
                            cmd = (Command) pkg.getData().loadClass(id).getConstructor().newInstance();
                        } catch (ReflectiveOperationException t) {
                            throw new PackageException("Unknown command: " + id);
                        }
                    }
                    cmd.initialize(element);
                    cmd.setPackageUpdateService(service);
                    commands.add(cmd);
                }
                node = node.getNextSibling();
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new PackageException("Failed to read commands", e);
        }
    }

}
