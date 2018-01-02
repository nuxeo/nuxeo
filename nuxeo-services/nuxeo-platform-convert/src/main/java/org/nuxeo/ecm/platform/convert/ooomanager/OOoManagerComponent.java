/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.convert.ooomanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeConnectionProtocol;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.artofsolving.jodconverter.office.OfficeTask;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @deprecated Since 8.4. See 'soffice' use with {@link org.nuxeo.ecm.platform.convert.plugins.CommandLineConverter} instead
 */
@Deprecated
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

    protected OOoState state = new OOoState();


    public OOoManagerDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONFIG_EP.equals(extensionPoint)) {
            OOoManagerDescriptor desc = (OOoManagerDescriptor) contribution;
            descriptor = desc;
        }
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
    public synchronized void stopOOoManager() throws Exception {
        if (state.isAlive()) {
            state.update(OOoState.STOPPING);
            officeManager.stop();
            state.update(OOoState.STOPPED);
            log.debug("Stopping ooo manager.");
        } else {
            log.debug("OOoManager already stopped..");
        }
    }

    @Override
    public synchronized void startOOoManager() throws Exception {
        startOOoManager(false);
    }

    public synchronized void startOOoManager(boolean async) throws Exception {
        if (!descriptor.isEnabled()) {
            return;
        }
        if (state.isAlive()) {
            log.info("OOo manager already started!");
            return;
        }
        state.update(OOoState.STARTING);
        log.info("Starting OOo manager");
        if (async) {
            Runnable oooStarter = new Runnable() {
                @Override
                public void run() {
                    try {
                        doStartOOoManager();
                    } catch (Exception e) {
                        log.error("Could not start OOoManager.", e);
                    }
                }
            };
            Thread oooStarterThread = new Thread(oooStarter);
            oooStarterThread.setDaemon(true);
            oooStarterThread.start();
        } else {
            doStartOOoManager();
        }
        log.info("Started OOo Manager");
    }

    private void doStartOOoManager() throws Exception {
        DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();

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
        String[] pipeNames;
        if (pipeNamesProperty != null) {
            String[] unvalidatedPipeNames = pipeNamesProperty.split(",\\s*");
            ArrayList<String> validatedPipeNames = new ArrayList<>();
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
        int[] portNumbers;
        if (portNumbersProperty != null) {
            String[] portStrings = portNumbersProperty.split(",\\s*");
            ArrayList<Integer> portList = new ArrayList<>();
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
            state.update(OOoState.STARTED);
            log.debug("Starting ooo manager.");
        } catch (Throwable e) {
            state.update(OOoState.STOPPED);
            Throwable t = unwrapException(e);
            log.warn("OpenOffice was not found, JOD Converter " + "won't be available: " + t.getMessage());
        }
    }

    public Throwable unwrapException(Throwable t) {
        Throwable cause = t.getCause();
        return cause == null ? t : unwrapException(cause);
    }

    @Override
    public void start(ComponentContext context) {
        try {
            startOOoManager(true);
        } catch (Exception e) {
            log.error("Failed to start OOoManager", e);
        }
    }

    @Override
    public void stop(ComponentContext context) {
        try {
            stopOOoManager();
        } catch (Exception e) {
            log.error("Failed to stop OOoManager.", e);
        }
    }

    @Override
    public boolean isOOoManagerStarted() {
        try {
            return state.isAlive();
        } catch (InterruptedException e) {
            log.warn("Thread interrupted while checkin OOo manager state");
            return false;
        }
    }

    public OfficeManager getOfficeManager() {
        return officeManager;
    }

    public OOoState getState() {
        return state;
    }

}
