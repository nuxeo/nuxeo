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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.guards.Guard;

/**
 * All commands have 2 attributes: fail and ignore which are EL expressions.
 * <p>
 * If ignore is defined and evaluated to true then the command will be ignored (null is returned as the inverse command)
 * If fail is defined and evaluated to true then the validation fails.
 * <p>
 * Commands extending this class must implement the {@link #doRun} and {@link #doValidate} methods instead of the one in
 * the interface. These methods are first testing for ignore and fail guards and then if needed delegated to the doXXX
 * method versions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractCommand implements Command {

    /**
     * List of files which must never be deleted at runtime.
     *
     * @since 5.5
     */
    protected final String id;

    protected final Map<String, Object> guardVars;

    protected String fail;

    protected String ignore;

    protected AbstractCommand(String id) {
        this.id = id;
        guardVars = new HashMap<>();
    }

    public AbstractCommand(AbstractCommand command) {
        this.id = command.id;
        guardVars = command.guardVars;
    }

    @Override
    public void setPackageUpdateService(PackageUpdateService packageUpdateService) {
        guardVars.put("packageUpdateService", packageUpdateService);
    }

    @Override
    public boolean isPostInstall() {
        return false;
    }

    /**
     * Override to implement command actions
     *
     * @return Rollback command
     */
    protected abstract Command doRun(Task task, Map<String, String> prefs) throws PackageException;

    /**
     * Override to implement validation.
     *
     * @param task The task being validated
     * @param status Use {@link ValidationStatus#addError(String)} or {@link ValidationStatus#addWarning(String)} to
     *            provide validation error/warning messages
     */
    protected abstract void doValidate(Task task, ValidationStatus status) throws PackageException;

    @Override
    public void validate(Task task, ValidationStatus status) throws PackageException {
        if (fail != null) {
            if (new Guard(fail).evaluate(guardVars)) {
                status.addError("Guard failed for command " + getId() + ": " + fail);
            }
        }
        doValidate(task, status);
    }

    @Override
    public Command run(Task task, Map<String, String> prefs) throws PackageException {
        if (ignore()) {
            return null;
        }
        return doRun(task, prefs);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setFail(String fail) {
        this.fail = fail;
    }

    public void setIgnore(String ignore) {
        this.ignore = ignore;
    }

    public boolean ignore() throws PackageException {
        if (ignore != null) {
            return new Guard(ignore).evaluate(guardVars);
        }
        return false;
    }

    @Override
    public void initialize(Element element) throws PackageException {
        String v = element.getAttribute("fail");
        if (v.length() > 0) {
            fail = v;
        }
        v = element.getAttribute("ignore");
        if (v.length() > 0) {
            ignore = v;
        }
        v = element.getAttribute("if");
        if (v.length() > 0) {
            ignore = String.format("!(%s)", v);
        }
        readFrom(element);
    }

    /**
     * Must be implemented to initialize the command arguments from an XML fragment.
     */
    public abstract void readFrom(Element element) throws PackageException;

}
