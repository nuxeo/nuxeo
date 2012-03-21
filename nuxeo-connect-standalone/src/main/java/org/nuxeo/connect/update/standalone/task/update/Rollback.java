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
package org.nuxeo.connect.update.standalone.task.update;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.standalone.task.AbstractTask;
import org.nuxeo.connect.update.standalone.task.commands.AbstractCommand;
import org.nuxeo.connect.update.standalone.task.commands.Command;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.nuxeo.connect.update.task.Task;
import org.w3c.dom.Element;

/**
 * 
 * @since 5.5
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Rollback extends AbstractCommand {

    protected static final Log log = LogFactory.getLog(Rollback.class);

    public static String ID = "rollback";

    protected String key;

    protected String version;

    protected boolean deleteOnExit;

    public Rollback() {
        super(ID);
    }

    public Rollback(RollbackOptions opt) {
        super(ID);
        this.key = opt.key;
        this.version = opt.version;
        this.deleteOnExit = opt.deleteOnExit;
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (version != null) {
            writer.attr("version", version);
        }
        if (key != null) {
            writer.attr("key", key);
        }
        if (deleteOnExit) {
            writer.attr("deleteOnExit", "true");
        }
        writer.end();
    }

    @Override
    public void readFrom(Element element) throws PackageException {
        String v = element.getAttribute("version");
        if (v.length() > 0) {
            version = v;
        }
        v = element.getAttribute("key");
        if (v.length() > 0) {
            key = v;
        }
        v = element.getAttribute("deleteOnExit");
        if (v.length() > 0) {
            deleteOnExit = Boolean.parseBoolean(v);
        }
    }

    @Override
    protected void doValidate(Task task, ValidationStatus status)
            throws PackageException {
        if (key == null || version == null) {
            status.addError("Cannot execute command in installer."
                    + " Invalid rollback syntax: key or version was not specified.");
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs)
            throws PackageException {

        UpdateManager mgr = ((AbstractTask) task).getUpdateManager();

        RollbackOptions opt = mgr.createRollbackOptions(
                task.getPackage().getId(), key, version);
        opt.setDeleteOnExit(deleteOnExit);

        mgr.rollback(opt);

        return null;
    }

}
