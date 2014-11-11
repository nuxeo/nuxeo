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
 * $Id: JBpmIdConverter.java 5175 2006-11-02 03:46:26Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.jbpm.util;

/**
 * Helper translating identifiers from NXWorkflow to jBPM and back.
 * <p>
 * Fairly simple for now, but might change depending on our needs.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class IDConverter {

    /**
     * Returns a NXWorkflow identifier from a jBPM identifier.
     *
     * @param identifier
     *            a jbpm identifier (long)
     * @return a NXWorkflow identifier as a string
     */
    public static String getNXWorkflowIdentifier(Long identifier) {
        return String.valueOf(identifier);
    }

    /**
     * Returns a jBPM identifier from a NXWorkflow one.
     *
     * @param identifier as a string
     * @return a Long for jBPM internals.
     */
    public static Long getJbpmIdentifier(String identifier) {
        return Long.parseLong(identifier);
    }

}
