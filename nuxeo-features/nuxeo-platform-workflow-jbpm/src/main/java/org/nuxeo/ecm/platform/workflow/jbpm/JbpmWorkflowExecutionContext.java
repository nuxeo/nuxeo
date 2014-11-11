/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: JbpmWorkflowExecutionContext.java 29517 2008-01-22 12:41:23Z atchertchian $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.db.GraphSession;
import org.jbpm.db.TaskMgmtSession;

/**
 * jBPM dedicated workflow execution context.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class JbpmWorkflowExecutionContext {

    private static String JBPM_CFG_XML = "jbpm.cfg.xml";

    private static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseResource(JBPM_CFG_XML);

    private final JbpmContext ctx;

    public JbpmWorkflowExecutionContext() {
        ctx = jbpmConfiguration.createJbpmContext();
    }

    /**
     * Gets the jBPM context.
     *
     * @return the jBPM context
     */
    public JbpmContext getContext() {
        return ctx;
    }

    /**
     * Closes the jBPM context.
     * <p>
     * It is the developer responsibility to close the workflow execution
     * session since this not possible to close automatically the context using
     * <code>finalize()</code>. The Hibernate session doesn't exist anymore
     * when the garbage collector is freeing this object.
     */
    public void closeContext() {
        if (ctx != null) {
            ctx.close();
        }
    }

    /**
     * Returns the corresponding jBPM GraphSession instance.
     *
     * @return the corresponding jBPM GraphSession instance
     */
    public GraphSession getGraphSession() {
        return ctx.getGraphSession();
    }

    /**
     * Returns the jBPM task manager session.
     *
     * @return the jBPM task manager session
     */
    public TaskMgmtSession getTaskMgmtSession() {
        return ctx.getTaskMgmtSession();
    }

    /**
     * Sets the configuration file and regenerates the configuration instance
     *
     * @param jbpm_cfg_xml
     */
    public static void setConfigurationFile(String jbpm_cfg_xml) {
        JBPM_CFG_XML = jbpm_cfg_xml;
        JbpmWorkflowExecutionContext.jbpmConfiguration = JbpmConfiguration.parseResource(JBPM_CFG_XML);
    }

    public static JbpmConfiguration getJbpmConfiguration() {
        return jbpmConfiguration;
    }

}
