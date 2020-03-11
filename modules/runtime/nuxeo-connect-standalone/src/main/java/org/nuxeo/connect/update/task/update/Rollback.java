/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.connect.update.task.update;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.standalone.AbstractTask;
import org.nuxeo.connect.update.task.standalone.commands.AbstractCommand;
import org.nuxeo.connect.update.task.standalone.commands.UndeployPlaceholder;
import org.nuxeo.connect.update.xml.XmlWriter;
import org.w3c.dom.Element;

/**
 * @since 5.5
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Rollback extends AbstractCommand {

    protected static final Log log = LogFactory.getLog(Rollback.class);

    public static final String ID = "rollback";

    protected String pkgId;

    protected String key;

    protected String version;

    protected boolean deleteOnExit;

    public Rollback() {
        super(ID);
    }

    public Rollback(RollbackOptions opt) {
        super(ID);
        this.pkgId = opt.pkgId;
        this.key = opt.key;
        this.version = opt.version;
        this.deleteOnExit = opt.deleteOnExit;
    }

    @Override
    public void writeTo(XmlWriter writer) {
        writer.start(ID);
        if (key != null) {
            writer.attr("key", key);
        }
        if (pkgId != null) {
            writer.attr("pkgId", pkgId);
        }
        if (version != null) {
            writer.attr("version", version);
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
        v = element.getAttribute("pkgId");
        if (v.length() > 0) {
            pkgId = v;
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
    protected void doValidate(Task task, ValidationStatus status) throws PackageException {
        // allow null version for Studio snapshot jar
        if (key == null) {
            status.addError("Cannot execute command in installer." + " Invalid rollback syntax: key was not specified.");
        }
    }

    @Override
    protected Command doRun(Task task, Map<String, String> prefs) throws PackageException {
        UpdateManager mgr = ((AbstractTask) task).getUpdateManager();
        RollbackOptions opt = new RollbackOptions(task.getPackage().getId(), key, version);
        File rollbackTarget = mgr.getRollbackTarget(opt);
        if (rollbackTarget != null) {
            Command undeploy = getUndeployCommand(rollbackTarget);
            if (undeploy != null) {
                undeploy.run(task, prefs);
            }
        }
        opt.setDeleteOnExit(deleteOnExit);
        mgr.rollback(opt);
        return null;
    }

    public RollbackOptions getRollbackOptions() {
        return new RollbackOptions(pkgId, key, version);
    }

    /**
     * Method to be overridden by subclasses to provide a undeploy command for hot reload
     *
     * @since 5.6
     */
    protected Command getUndeployCommand(File targetFile) {
        return new UndeployPlaceholder(targetFile);
    }

}
