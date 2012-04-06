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

package org.nuxeo.connect.update.task.standalone.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.UninstallTask;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 * Rollback command for {@link Append} and {@link Copy} (with
 * {@link Copy#append}=true) commands.
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
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        BufferedReader brToRemove, brFromFile;
        File bak;
        try {
            brToRemove = new BufferedReader(new FileReader(contentToRemove));
            String lineToRemove = brToRemove.readLine();
            brFromFile = new BufferedReader(new FileReader(fromFile));
            String lineToCheck;
            StringBuilder linesToKeep = new StringBuilder();
            StringBuilder linesToRemove = new StringBuilder();
            boolean found = false;
            while ((lineToCheck = brFromFile.readLine()) != null) {

                if (lineToCheck.equals(lineToRemove)) {
                    // Maybe the line to remove, but let's check the next lines
                    found = true;
                    linesToRemove.append(lineToCheck + newLine);
                    lineToRemove = brToRemove.readLine();
                } else {
                    if (lineToRemove != null && found) {
                        // Previously found lines must finally be kept
                        found = false;
                        linesToKeep.append(linesToRemove.toString());
                        linesToRemove = new StringBuilder();
                        org.apache.commons.io.IOUtils.closeQuietly(brToRemove);
                        brToRemove = new BufferedReader(new FileReader(
                                contentToRemove));
                    }
                    linesToKeep.append(lineToCheck + newLine);
                }
            }
            if (lineToRemove != null) {
                throw new PackageException(
                        "All lines to remove were not found.");
            }
            org.apache.commons.io.IOUtils.closeQuietly(brToRemove);
            org.apache.commons.io.IOUtils.closeQuietly(brFromFile);
            if (task instanceof UninstallTask) {
                bak = null;
            } else {
                bak = IOUtils.backup(task.getPackage(), contentToRemove);
            }
            FileUtils.writeFile(fromFile, linesToKeep.toString());
            return new Append(bak, fromFile);
        } catch (Exception e) {
            throw new PackageException(e);
        }
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
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
