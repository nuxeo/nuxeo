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

import org.nuxeo.common.utils.FileUtils;
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
 * Copy a file to the given target directory or file. If the target is a
 * directory the file name is preserved. If the target file exists it will be
 * replaced if overwrite is true otherwise the command validation fails.
 * <p>
 * If md5 is set then the copy command will be validated only if the target file
 * has the same md5 as the one specified in the command.
 * <p>
 * The Copy command has as inverse either Delete either another Copy command. If
 * the file was copied without overwriting then Delete is the inverse (with a
 * md5 set to the one of the copied file). If the file was overwritten then the
 * Copy command has an inverse another copy command with the md5 to the one of
 * the copied file and the overwrite flag to true. The file to copy will be the
 * backup of the overwritten file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Copy extends AbstractCommand {

    public static final String ID = "copy";

    protected File file;

    /**
     * the target file - cannot be a directory
     */
    protected File tofile;

    protected boolean overwrite;

    protected String md5;

    protected Copy(String id) {
        super(id);
    }

    public Copy() {
        super(ID);
    }

    public Copy(File file, File tofile, String md5, boolean overwrite) {
        this(ID, file, tofile, md5, overwrite);
    }

    protected Copy(String id, File file, File tofile, String md5,
            boolean overwrite) {
        super(id);
        this.file = file;
        this.tofile = tofile;
        this.md5 = md5;
        this.overwrite = overwrite;
    }

    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        String md5 = null;
        File bak = null;
        File dst = null;
        try {
            // backup the destination file if exist.
            if (tofile.isFile()) {
                if (!overwrite) { // force a rollback
                    throw new PackageException(
                            "Copy command has override flag on false but destination file exists: "
                                    + tofile);
                }
                bak = IOUtils.backup(task.getPackage(), tofile);
                dst = tofile;
            } else if (tofile.isDirectory()) {
                dst = new File(tofile, file.getName());
                if (dst.isFile()) {
                    bak = IOUtils.backup(task.getPackage(), dst);
                }
            } else { // target file doesn't exists - it will be created
                tofile.getParentFile().mkdirs();
                dst = tofile;
            }
            // copy the file - use getContentToCopy to allow parametrization for
            // subclasses
            String content = getContentToCopy(prefs);
            if (content != null) {
                FileUtils.writeFile(dst, content);
            } else {
                FileUtils.copy(file, dst);
            }
            // get the md5 of the copied file.
            md5 = IOUtils.createMd5(dst);
        } catch (Exception e) {
            throw new PackageException("Failed to copy " + dst, e);
        }
        if (bak == null) { // no file was replaced
            return new Delete(dst, md5);
        } else {
            return new Copy(bak, dst, md5, true);
        }
    }

    protected String getContentToCopy(Map<String, String> prefs)
            throws PackageException {
        return null;
    }

    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        if (file == null || tofile == null) {
            status.addError("Cannot execute command in installer. No file or tofile specified.");
        }
        if (tofile.isFile() && !overwrite) {
            status.addError("Cannot overwite existing file: "
                    + tofile.getName());
        }
        if (md5 != null) {
            try {
                if (tofile.isFile() && !md5.equals(IOUtils.createMd5(file))) {
                    status.addError("Cannot copy file to: " + tofile.getName()
                            + ". The md5 check failed");
                } else {
                    status.addError("MD5 set but tofile doesn't exists. Cannot perform copy over: "
                            + tofile.getName());
                }
            } catch (Exception e) {
                throw new PackageException(e);
            }
        }
    }

    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("file");
        if (v.length() > 0) {
            file = new File(v);
        } else {
            throw new PackageException(
                    "Invalid copy syntax: file was not specified");
        }
        v = element.getAttribute("todir");
        if (v.length() > 0) {
            tofile = new File(v, file.getName());
            guardVars.put("tofile", tofile);
        } else {
            v = element.getAttribute("tofile");
            if (v.length() > 0) {
                FileRef ref = FileRef.newFileRef(v);
                tofile = ref.getFile();
                guardVars.put("tofile", tofile);
                ref.fillPatternVariables(guardVars);
            }
        }
        v = element.getAttribute("md5");
        if (v.length() > 0) {
            md5 = v;
        }
        v = element.getAttribute("overwrite");
        if (v.length() > 0) {
            overwrite = Boolean.parseBoolean(v);
        }
    }

    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        if (tofile != null) {
            writer.attr("tofile", tofile.getAbsolutePath());
        }
        writer.attr("overwrite", String.valueOf(overwrite));
        if (md5 != null) {
            writer.attr("md5", md5);
        }
        writer.end();
    }

}
