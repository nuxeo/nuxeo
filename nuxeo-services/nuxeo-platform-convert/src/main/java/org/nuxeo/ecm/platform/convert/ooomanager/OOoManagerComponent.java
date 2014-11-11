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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.artofsolving.jodconverter.office.OfficeUtils;
import org.artofsolving.jodconverter.util.PlatformUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class OOoManagerComponent extends DefaultComponent implements
        OOoManagerService {

    protected static final Log log = LogFactory.getLog(OOoManagerComponent.class);

    private static final String CONNECTION_PROTOCOL_PROPERTY_KEY = "jod.connection.protocol";

    private static final String MAX_TASKS_PER_PROCESS_PROPERTY_KEY = "jod.max.tasks.per.process";

    private static final String OFFICE_HOME_PROPERTY_KEY = "jod.office.home";

    private static final String TASK_EXECUTION_TIMEOUT_PROPERTY_KEY = "jod.task.execution.timeout";

    private static final String TASK_QUEUE_TIMEOUT_PROPERTY_KEY = "jod.task.queue.timeout";

    private static final String TEMPLATE_PROFILE_DIR_PROPERTY_KEY = "jod.template.profile.dir";

    private static final String DEFAULT_LINUX_OO_HOME = "/usr/lib/openoffice/";

    protected static String CONFIG_EP = "oooManagerConfig";

    private static OfficeManager officeManager;

    protected OOoManagerDescriptor descriptor = new OOoManagerDescriptor();

    protected boolean started = false;

    public OOoManagerDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equals(extensionPoint)) {
            OOoManagerDescriptor desc = (OOoManagerDescriptor) contribution;
            descriptor = desc;
        }
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        stopOOoManager();
    }

    public OfficeDocumentConverter getDocumentConverter() {
        if (isOOoManagerStarted()) {
            return new OfficeDocumentConverter(officeManager);
        } else {
            log.error("OfficeManager is not started.");
            return null;
        }
    }

    public void stopOOoManager() {
        if (started) {
            officeManager.stop();
            log.debug("Stoping ooo manager.");
        } else {
            log.debug("OOoManager already stoped..");
        }
    }

    public void startOOoManager() throws IOException {
        DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();

        // Properties configuration
        String connectionProtocol = Framework.getProperty(CONNECTION_PROTOCOL_PROPERTY_KEY);
        if (connectionProtocol != null && !"".equals(connectionProtocol)) {
            if (OfficeConnectionProtocol.PIPE.toString().equals(
                    connectionProtocol)) {
                ConfigBuilderHelper.hackClassLoader();
                configuration.setConnectionProtocol(OfficeConnectionProtocol.PIPE);
            } else if (OfficeConnectionProtocol.SOCKET.toString().equals(
                    connectionProtocol)) {
                configuration.setConnectionProtocol(OfficeConnectionProtocol.SOCKET);
            }
        }
        String maxTasksPerProcessProperty = Framework.getProperty(MAX_TASKS_PER_PROCESS_PROPERTY_KEY);
        if (maxTasksPerProcessProperty != null
                && !"".equals(maxTasksPerProcessProperty)) {
            Integer maxTasksPerProcess = Integer.valueOf(maxTasksPerProcessProperty);
            configuration.setMaxTasksPerProcess(maxTasksPerProcess);
        }
        String officeHome = Framework.getProperty(OFFICE_HOME_PROPERTY_KEY);
        if (officeHome != null && !"".equals(officeHome)) {
            configuration.setOfficeHome(officeHome);
        } else {
            if (PlatformUtils.isLinux()) {
                File officeHomeDirectory = new File(DEFAULT_LINUX_OO_HOME);
                if (officeHomeDirectory.isDirectory()) {
                    File officeExecutable = OfficeUtils.getOfficeExecutable(officeHomeDirectory);
                    if (officeExecutable.exists()) {
                        configuration.setOfficeHome(DEFAULT_LINUX_OO_HOME);
                    }
                    officeExecutable = null;
                }
                officeHomeDirectory = null;
            }
        }

        String taskExecutionTimeoutProperty = Framework.getProperty(TASK_EXECUTION_TIMEOUT_PROPERTY_KEY);
        if (taskExecutionTimeoutProperty != null
                && !"".equals(taskExecutionTimeoutProperty)) {
            Long taskExecutionTimeout = Long.valueOf(taskExecutionTimeoutProperty);
            configuration.setTaskExecutionTimeout(taskExecutionTimeout);
        }
        String taskQueueTimeoutProperty = Framework.getProperty(TASK_QUEUE_TIMEOUT_PROPERTY_KEY);
        if (taskQueueTimeoutProperty != null
                && !"".equals(taskQueueTimeoutProperty)) {
            Long taskQueueTimeout = Long.valueOf(taskQueueTimeoutProperty);
            configuration.setTaskQueueTimeout(taskQueueTimeout);
        }
        String templateProfileDir = Framework.getProperty(TEMPLATE_PROFILE_DIR_PROPERTY_KEY);
        if (templateProfileDir != null && !"".equals(templateProfileDir)) {
            File templateDirectory = new File(templateProfileDir);
            if (!templateDirectory.exists()) {
                try {
                    FileUtils.forceMkdir(templateDirectory);
                } catch (IOException e) {
                    throw new RuntimeException(
                            "I/O Error: could not create JOD templateDirectory");
                }
            }
            configuration.setTemplateProfileDir(templateDirectory);
        }

        // Descriptor configuration
        String[] pipeNames = descriptor.getPipeNames();
        if (pipeNames != null && pipeNames.length != 0) {
            configuration.setPipeNames(pipeNames);
        }
        int[] portNumbers = descriptor.getPortNumbers();
        if (portNumbers != null && portNumbers.length != 0) {
            configuration.setPortNumbers(portNumbers);
        }
        try {
            officeManager = configuration.buildOfficeManager();
            officeManager.start();
            started = true;
            log.debug("Starting ooo manager.");
        } catch (Exception e) {
            Throwable t = unwrapException(e);
            log.warn("OpenOffice was not found, JOD Converter "
                    + "won't be available: " + t.getMessage());
        }
    }

    public Throwable unwrapException(Throwable t) {
        Throwable cause = null;

        if (t instanceof ServletException) {
            cause = ((ServletException) t).getRootCause();
        } else if (t instanceof ClientException) {
            cause = t.getCause();
        } else if (t instanceof Exception) {
            cause = t.getCause();
        }

        if (cause == null) {
            return t;
        } else {
            return unwrapException(cause);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        try {
            startOOoManager();
        } catch (IOException e) {
            throw new RuntimeException("Could not start OOoManager.", e);
        }
    }

    public boolean isOOoManagerStarted() {
        return started;
    }

}
