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
package org.nuxeo.connect.update.task.standalone.commands;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.connect.update.PackageException;
import org.nuxeo.connect.update.ValidationStatus;
import org.nuxeo.connect.update.task.Command;
import org.nuxeo.connect.update.task.Task;
import org.nuxeo.connect.update.task.guards.Guard;
import org.w3c.dom.Element;

/**
 * All commands have 2 attributes: fail and ignore which are EL expressions.
 * <p>
 * If ignore is defined and evaluated to true then the command will be ignored
 * (null is returned as the inverse command) If fail is defined and evaluated to
 * true then the validation fails.
 * <p>
 * Commands extending this class must implement the {@link #doRun} and
 * {@link #doValidate} methods instead of the one in the interface. These
 * methods are first testing for ignore and fail guards and then if needed
 * delegated to the doXXX method versions.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractCommand implements Command {

    /**
     * List of files which must never be deleted at runtime.
     *
     * @since 5.5
     */
    public static final String[] FILES_TO_DELETE_ONLY_ON_EXIT = { "nuxeo-core-storage-sql" };

    protected final String id;

    protected final Map<String, Object> guardVars;

    protected String fail;

    protected String ignore;

    protected AbstractCommand(String id) {
        this.id = id;
        guardVars = new HashMap<String, Object>();
    }

    @Override
    public boolean isPostInstall() {
        return false;
    }

    /**
     * Override to implement command actions
     *
     * @param task
     * @param prefs
     * @return Rollback command
     * @throws PackageException
     */
    protected abstract Command doRun(Task task, Map<String, String> prefs)
            throws PackageException;

    /**
     * Override to implement validation.
     *
     * @param task The task being validated
     * @param status Use {@link ValidationStatus#addError(String)} or
     *            {@link ValidationStatus#addWarning(String)} to provide
     *            validation error/warning messages
     * @throws PackageException
     */
    protected abstract void doValidate(Task task, ValidationStatus status)
            throws PackageException;

    public void validate(Task task, ValidationStatus status)
            throws PackageException {
        if (fail != null) {
            try {
                if (new Guard(fail).evaluate(guardVars)) {
                    status.addError("Guard failed for command " + getId()
                            + ": " + fail);
                }
            } catch (Exception e) {
                throw new PackageException("Ignore guard failed: ", e);
            }
        }
        doValidate(task, status);
    }

    public Command run(Task task, Map<String, String> prefs)
            throws PackageException {
        if (ignore()) {
            return null;
        }
        return doRun(task, prefs);
    }

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
            try {
                return new Guard(ignore).evaluate(guardVars);
            } catch (Exception e) {
                throw new PackageException("Ignore guard failed: ", e);
            }
        }
        return false;
    }

    public void initialize(Element element) throws PackageException {
        String v = element.getAttribute("fail");
        if (v.length() > 0) {
            fail = v;
        }
        v = element.getAttribute("ignore");
        if (v.length() > 0) {
            ignore = v;
        }
        readFrom(element);
    }

    /**
     * Must be implemented to initialize the command arguments from an XML
     * fragment.
     */
    public abstract void readFrom(Element element) throws PackageException;

}
