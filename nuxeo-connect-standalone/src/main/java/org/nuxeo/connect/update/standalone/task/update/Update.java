/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.connect.update.standalone.task.update;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.standalone.task.AbstractTask;
import org.nuxeo.connect.update.standalone.task.commands.AbstractCommand;
import org.nuxeo.connect.update.standalone.task.commands.CompositeCommand;
import org.nuxeo.connect.update.standalone.task.update.JarUtils.Match;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 *
 * @since 5.5
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Update extends AbstractCommand {

    protected static final Log log = LogFactory.getLog(Update.class);

    public static final String ID = "update";

    /**
     * The source file. It can be a file or a directory.
     */
    private File file;

    /**
     * The target file. It can be a directory since 5.5
     */
    private File todir;

    private boolean removeOnExit;

    private boolean allowDowngrade = false;

    private boolean upgradeOnly = false;

    protected Update(String id) {
        super(id);
    }

    public Update() {
        this(ID);
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        File dir = null;
        String v = element.getAttribute("dir");
        if (v.length() > 0) {
            dir = new File(v);
        }
        v = element.getAttribute("file");
        if (v.length() > 0) {
            if (dir != null) {
                file = new File(dir, v);
            } else {
                file = new File(v);
            }
            guardVars.put("file", file);
        } else {
            file = dir;
            guardVars.put("dir", dir);
        }

        v = element.getAttribute("todir");
        if (v.length() > 0) {
            todir = new File(v);
            guardVars.put("todir", todir);
        }

        v = element.getAttribute("removeOnExit");
        if (v.length() > 0) {
            removeOnExit = Boolean.parseBoolean(v);
        }
        v = element.getAttribute("allowDowngrade");
        if (v.length() > 0) {
            allowDowngrade = Boolean.parseBoolean(v);
        }
        v = element.getAttribute("upgradeOnly");
        if (v.length() > 0) {
            upgradeOnly = Boolean.parseBoolean(v);
        }
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (file != null) {
            writer.attr("file", file.getAbsolutePath());
        }
        if (todir != null) {
            writer.attr("todir", todir.getAbsolutePath());
        }
        if (removeOnExit) {
            writer.attr("removeOnExit", "true");
        }
        if (allowDowngrade) {
            writer.attr("allowDowngrade", "true");
        }
        if (upgradeOnly) {
            writer.attr("upgradeOnly", "true");
        }
        writer.end();
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        if (file == null || todir == null) {
            status.addError("Cannot execute command in installer."
                    + " Invalid update syntax: file or todir was not specified.");
        }
        if (todir.isFile()) {
            status.addError("Cannot execute command in installer."
                    + " Invalid update command: todir should be a directory!");
        }
        if (file.isFile()) {
            Match<String> match = JarUtils.findJarVersion(file.getName());
            if (match == null) {
                status.addError("Cannot execute command in installer."
                        + " Cannot use 'update' command for non versioned files!. File name must contain a version: "
                        + file.getName());
            }
        } else if (!file.isDirectory()) {
            status.addWarning("Ignored command in installer."
                    + " Source file not found! " + file.getName());
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {
        if (!file.exists()) {
            log.warn("Can't update using " + file + " . File is missing.");
            return null;
        }
        UpdateManager mgr = ((AbstractTask) task).getUpdateManager();

        if (file.isDirectory()) {
            return updateDirectory(task, file, mgr);
        } else {
            return updateFile(task, file, mgr);
        }

    }

    protected CompositeCommand updateDirectory(Task task, File dir,
            UpdateManager mgr) throws PackageException {
        CompositeCommand cmd = new CompositeCommand();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                cmd.addCommand(updateFile(task, file, mgr));
            }
        }
        return cmd;
    }

    protected Rollback updateFile(Task task, File file, UpdateManager mgr)
            throws PackageException {
        UpdateOptions opt = mgr.createUpdateOptions(task.getPackage().getId(),
                file, todir);
        if (opt == null) {
            return null;
        }
        opt.setAllowDowngrade(allowDowngrade);
        opt.setUpgradeOnly(upgradeOnly);
        computeRemoveOnExit(opt);
        try {
            RollbackOptions r = mgr.update(opt);
            return new Rollback(r);
        } catch (VersionAlreadyExistException e) {
            // should never happens
            return null;
        }
    }

    public void computeRemoveOnExit(UpdateOptions opt) {
        boolean force = ArrayUtils.contains(FILES_TO_DELETE_ONLY_ON_EXIT,
                opt.nameWithoutVersion);
        if (force) {
            opt.deleteOnExit = true;
        } else {
            opt.deleteOnExit = removeOnExit;
        }
    }

}
