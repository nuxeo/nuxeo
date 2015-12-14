/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.convert.ooomanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.artofsolving.jodconverter.office.OfficeTask;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class OOoManagerComponent extends DefaultComponent implements OOoManagerService {

    protected static final Log log = LogFactory.getLog(OOoManagerComponent.class);

    private static final String CONNECTION_PROTOCOL_PROPERTY_KEY = "jod.connection.protocol";

    private static final String MAX_TASKS_PER_PROCESS_PROPERTY_KEY = "jod.max.tasks.per.process";

    private static final String OFFICE_HOME_PROPERTY_KEY = "jod.office.home";

    private static final String TASK_EXECUTION_TIMEOUT_PROPERTY_KEY = "jod.task.execution.timeout";

    private static final String TASK_QUEUE_TIMEOUT_PROPERTY_KEY = "jod.task.queue.timeout";

    private static final String TEMPLATE_PROFILE_DIR_PROPERTY_KEY = "jod.template.profile.dir";

    private static final String OFFICE_PIPES_PROPERTY_KEY = "jod.office.pipes";

    private static final String OFFICE_PORTS_PROPERTY_KEY = "jod.office.ports";

    protected static final String CONFIG_EP = "oooManagerConfig";

    private static OfficeManager officeManager;

    protected OOoManagerDescriptor descriptor = new OOoManagerDescriptor();

    protected boolean started = false;

    protected boolean starting = false;

    protected boolean shutingdown = false;

    public OOoManagerDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor)
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

    @Override
    public OfficeDocumentConverter getDocumentConverter() {
        if (isOOoManagerStarted()) {
            return new OfficeDocumentConverter(officeManager);
        } else {
            log.error("OfficeManager is not started.");
            return null;
        }
    }

    public void executeTask(OfficeTask task) {
        if (isOOoManagerStarted()) {
            officeManager.execute(task);
        } else {
            log.error("OfficeManager is not started.");
        }
    }

    @Override
    public void stopOOoManager() {
        if (isOOoManagerStarted() && !shutingdown) {
            shutingdown = true;
            officeManager.stop();
            started = false;
            shutingdown = false;
            log.debug("Stopping ooo manager.");
        } else {
            log.debug("OOoManager already stopped..");
        }
    }

    @Override
    public void startOOoManager() throws IOException {
        DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();

        starting = true;

        try {
            // Properties configuration
            String connectionProtocol = Framework.getProperty(CONNECTION_PROTOCOL_PROPERTY_KEY);
            if (connectionProtocol != null && !"".equals(connectionProtocol)) {
                if (OfficeConnectionProtocol.PIPE.toString().equals(connectionProtocol)) {
                    ConfigBuilderHelper.hackClassLoader();
                    configuration.setConnectionProtocol(OfficeConnectionProtocol.PIPE);
                } else if (OfficeConnectionProtocol.SOCKET.toString().equals(connectionProtocol)) {
                    configuration.setConnectionProtocol(OfficeConnectionProtocol.SOCKET);
                }
            }
            String maxTasksPerProcessProperty = Framework.getProperty(MAX_TASKS_PER_PROCESS_PROPERTY_KEY);
            if (maxTasksPerProcessProperty != null && !"".equals(maxTasksPerProcessProperty)) {
                Integer maxTasksPerProcess = Integer.valueOf(maxTasksPerProcessProperty);
                configuration.setMaxTasksPerProcess(maxTasksPerProcess);
            }
            String officeHome = Framework.getProperty(OFFICE_HOME_PROPERTY_KEY);
            if (officeHome != null && !"".equals(officeHome)) {
                configuration.setOfficeHome(officeHome);
            }

            String taskExecutionTimeoutProperty = Framework.getProperty(TASK_EXECUTION_TIMEOUT_PROPERTY_KEY);
            if (taskExecutionTimeoutProperty != null && !"".equals(taskExecutionTimeoutProperty)) {
                Long taskExecutionTimeout = Long.valueOf(taskExecutionTimeoutProperty);
                configuration.setTaskExecutionTimeout(taskExecutionTimeout);
            }
            String taskQueueTimeoutProperty = Framework.getProperty(TASK_QUEUE_TIMEOUT_PROPERTY_KEY);
            if (taskQueueTimeoutProperty != null && !"".equals(taskQueueTimeoutProperty)) {
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
                        throw new RuntimeException("I/O Error: could not create JOD templateDirectory");
                    }
                }
                configuration.setTemplateProfileDir(templateDirectory);
            }

            // Descriptor configuration
            String pipeNamesProperty = Framework.getProperty(OFFICE_PIPES_PROPERTY_KEY);
            String[] pipeNames = null;
            if (pipeNamesProperty != null) {
                String[] unvalidatedPipeNames = pipeNamesProperty.split(",\\s*");
                ArrayList<String> validatedPipeNames = new ArrayList<String>();
                // Basic validation to avoid empty strings
                for (int i = 0; i < unvalidatedPipeNames.length; i++) {
                    String tmpPipeName = unvalidatedPipeNames[i].trim();
                    if (tmpPipeName.length() > 0) {
                        validatedPipeNames.add(tmpPipeName);
                    }
                }
                pipeNames = validatedPipeNames.toArray(new String[0]);
            } else {
                pipeNames = descriptor.getPipeNames();
            }
            if (pipeNames != null && pipeNames.length != 0) {
                configuration.setPipeNames(pipeNames);
            }
            String portNumbersProperty = Framework.getProperty(OFFICE_PORTS_PROPERTY_KEY);
            int[] portNumbers = null;
            if (portNumbersProperty != null) {
                String[] portStrings = portNumbersProperty.split(",\\s*");
                ArrayList<Integer> portList = new ArrayList<Integer>();
                for (int i = 0; i < portStrings.length; i++) {
                    try {
                        portList.add(Integer.parseInt(portStrings[i].trim()));
                    } catch (NumberFormatException e) {
                        log.error("Ignoring malformed port number: " + portStrings[i]);
                    }
                }
                portNumbers = ArrayUtils.toPrimitive(portList.toArray(new Integer[0]));
            } else {
                portNumbers = descriptor.getPortNumbers();
            }
            if (portNumbers != null && portNumbers.length != 0) {
                configuration.setPortNumbers(portNumbers);
            }
            try {
                officeManager = configuration.buildOfficeManager();
                officeManager.start();
                started = true;
                log.debug("Starting ooo manager.");
            } catch (Exception e) {
                started = false;
                Throwable t = unwrapException(e);
                log.warn("OpenOffice was not found, JOD Converter " + "won't be available: " + t.getMessage());
            }
        } finally {
            starting = false;
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
        log.info("Starting OOo manager");
        Runnable oooStarter = new Runnable() {
            @Override
            public void run() {
                try {
                    startOOoManager();
                } catch (IOException e) {
                    log.error("Could not start OOoManager.", e);
                }
            }
        };
        Thread oooStarterThread = new Thread(oooStarter);
        oooStarterThread.setDaemon(true);
        oooStarterThread.start();
        log.info("Started OOo Manager");
    }

    @Override
    public boolean isOOoManagerStarted() {
        if (shutingdown) {
            return false;
        }
        if (!starting) {
            return started;
        }

        // wait a little bit
        // while we are starting Ooo
        for (int i = 0; i < 200; i++) {
            if (starting) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // NOP
                }
            }
            if (!starting) {
                return started;
            }
        }

        log.error("Timeout on waiting for officeManager to start");

        return started;
    }

    public OfficeManager getOfficeManager() {
        return officeManager;
    }
}
