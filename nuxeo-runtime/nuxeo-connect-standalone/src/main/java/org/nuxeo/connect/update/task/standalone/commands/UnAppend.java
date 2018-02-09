/*
 * (C) Copyright 2011-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.connect.update.task.standalone.commands;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 * Rollback command for {@link Append} and {@link Copy} (with {@link Copy#append}=true) commands.
 *
 * @since 5.5
 */
public class UnAppend extends AbstractCommand {

    public static final String ID = "unappend";

    private static final String newLine = System.getProperty("line.separator");

    private File contentToRemove;

    private File fromFile;

    public UnAppend() {
        this(ID);
    }

    protected UnAppend(String id) {
        super(id);
    }

    /**
     * @param contentToRemove File which content must be removed.
     * @param fromFile Destination file from which content is removed.
     */
    public UnAppend(File contentToRemove, File fromFile) {
        this(ID);
        this.contentToRemove = contentToRemove;
        this.fromFile = fromFile;
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (contentToRemove != null) {
            writer.attr("contentToRemove", contentToRemove.getAbsolutePath());
        }
        if (fromFile != null) {
            writer.attr("fromFile", fromFile.getAbsolutePath());
        }
        writer.end();
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        BufferedReader brToRemove = null, brFromFile = null;
        File bak;
        StringBuilder linesToKeep = new StringBuilder();
        StringBuilder linesToRemove = new StringBuilder();
        try {
            try {
                brToRemove = new BufferedReader(new FileReader(contentToRemove));
                String lineToRemove = brToRemove.readLine();
                brFromFile = new BufferedReader(new FileReader(fromFile));
                String lineToCheck;
                boolean found = false;
                while ((lineToCheck = brFromFile.readLine()) != null) {

                    if (lineToCheck.equals(lineToRemove)) {
                        // Maybe the line to remove, but let's check the next
                        // lines
                        found = true;
                        linesToRemove.append(lineToCheck).append(newLine);
                        lineToRemove = brToRemove.readLine();
                    } else {
                        if (lineToRemove != null && found) {
                            // Previously found lines must finally be kept
                            found = false;
                            linesToKeep.append(linesToRemove.toString());
                            linesToRemove = new StringBuilder();
                            org.apache.commons.io.IOUtils.closeQuietly(brToRemove);
                            brToRemove = new BufferedReader(new FileReader(contentToRemove));
                        }
                        linesToKeep.append(lineToCheck).append(newLine);
                    }
                }
                if (lineToRemove != null) {
                    throw new PackageException("All lines to remove were not found.");
                }
            } finally {
                org.apache.commons.io.IOUtils.closeQuietly(brToRemove);
                org.apache.commons.io.IOUtils.closeQuietly(brFromFile);
            }
            if (task instanceof UninstallTask) {
                bak = null;
            } else {
                bak = IOUtils.backup(task.getPackage(), contentToRemove);
            }
            FileUtils.writeStringToFile(fromFile, linesToKeep.toString(), UTF_8);
            return new Append(bak, fromFile);
        } catch (PackageException e) {
            throw e;
        } catch (IOException e) {
            throw new PackageException(e);
        }
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status) throws PackageException {
        if (contentToRemove == null || fromFile == null) {
            status.addError("Cannot execute command in installer."
                    + " Invalid unappend syntax: contentToRemove or fromFile was not specified.");
        }
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("contentToRemove");
        if (v.length() > 0) {
            contentToRemove = new File(v);
        }
        v = element.getAttribute("fromFile");
        if (v.length() > 0) {
            fromFile = new File(v);
        }
    }

}
