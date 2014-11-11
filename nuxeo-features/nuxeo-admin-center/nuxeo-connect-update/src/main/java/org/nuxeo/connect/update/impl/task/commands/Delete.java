/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.impl.task.commands;

import java.io.File;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.impl.task.AbstractCommand;
import org.nuxeo.connect.update.impl.task.Command;
import org.nuxeo.connect.update.impl.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.util.FileRef;
import org.nuxeo.connect.update.util.IOUtils;
import org.w3c.dom.Element;

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

    public static final String ID = "delete";

    protected File file; // the file to restore

    protected String md5;

    public Delete() {
        super(ID);
    }

    public Delete(File file, String md5) {
        super(ID);
        this.file = file;
        this.md5 = md5;
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
                    return null; // ignore the command since the md5 doesn't
                    // match
                }
                File bak = IOUtils.backup(task.getPackage(), file);
                file.delete();
                return new Copy(bak, file, md5, false);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new PackageException(
                    "Failed to create backup when deleting: " + file.getName());
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
    }

    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        if (md5 != null) {
            writer.attr("md5", md5);
        }
        writer.end();
    }
}
