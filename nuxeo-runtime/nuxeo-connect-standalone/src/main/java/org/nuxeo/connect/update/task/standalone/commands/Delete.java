/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.connect.update.task.standalone.commands;

import java.io.File;
import java.io.IOException;
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
 * The delete command. This command takes 2 arguments: the file path to delete and an optional md5. If md5 is set then
 * the command fails if the target file has not the same md5.
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

    @Override
    protected void doValidate(Task task, ValidationStatus status) {
        if (file == null) {
            status.addError("Invalid delete syntax: No file specified");
            return;
        }
        if (file.isDirectory()) {
            status.addError("Cannot delete directories: " + file.getName());
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
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
                        throw new PackageException("Cannot delete " + file.getName());
                    }
                }
                return new Copy(bak, file, md5, false, onExit);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new PackageException("Failed to create backup when deleting: " + file.getName(), e);
        }
    }

    @Override
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

    @Override
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
