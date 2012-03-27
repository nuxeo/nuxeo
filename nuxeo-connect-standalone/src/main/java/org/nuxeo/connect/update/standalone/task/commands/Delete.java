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
 *     bstefanescu
 */
package org.nuxeo.connect.update.standalone.task.commands;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import org.nuxeo.common.utils.FileRef;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.IOUtils;
import org.nuxeo.connect.update.xml.XmlWriter;

/**
 * The delete command. This command takes 2 arguments: the file path to delete
 * and an optional md5. Of md5 is set then the command fails id the target file
 * has not the same md5.
 * <p>
 * The inverse of the delete command is a copy command.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Delete extends AbstractCommand {

    protected static final Log log = LogFactory.getLog(Delete.class);

    public static final String ID = "delete";

    protected File file; // the file to restore

    protected String md5;

    protected boolean onExit;

    public Delete() {
        super(ID);
    }

    public Delete(File file, String md5) {
        this(file, md5, false);
    }

    public Delete(File file, String md5, boolean onExit) {
        super(ID);
        this.file = file;
        this.md5 = md5;
        this.onExit = onExit;
    }

    protected void doValidate(Task task, ValidationStatus status) {
        if (file == null) {
            status.addError("Invalid delete syntax: No file specified");
        }
        if (file.isDirectory()) {
            status.addError("Cannot delete directories: " + file.getName());
        }
    }

    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        try {
            if (file.isFile()) {
                if (md5 != null && !md5.equals(IOUtils.createMd5(file))) {
                    // ignore the command since the md5 doesn't match
                    return null;
                }
                File bak = IOUtils.backup(task.getPackage(), file);
                if (onExit) {
                    file.deleteOnExit();
                } else {
                    if (!file.delete()) {
                        throw new PackageException("Cannot delete "
                                + file.getName());
                    }
                }
                return new Copy(bak, file, md5, false, onExit);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new PackageException(
                    "Failed to create backup when deleting: " + file.getName(), e);
        }
    }

    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("file");
        if (v.length() > 0) {
            FileRef ref = FileRef.newFileRef(v);
            ref.fillPatternVariables(guardVars);
            file = ref.getFile();
            guardVars.put("file", file);
        }
        v = element.getAttribute("md5");
        if (v.length() > 0) {
            md5 = v;
        }
        v = element.getAttribute("onExit");
        if (v.length() > 0) {
            onExit = Boolean.parseBoolean(v);
        }
    }

    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        if (md5 != null) {
            writer.attr("md5", md5);
        }
        if (onExit) {
            writer.attr("onExit", "true");
        }
        writer.end();
    }
}
