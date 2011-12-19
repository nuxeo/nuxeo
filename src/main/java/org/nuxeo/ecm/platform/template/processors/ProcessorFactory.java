/*
 * (C) Copyright 2006-20012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.template.processors;

import org.nuxeo.ecm.platform.template.processors.docx.WordXMLTemplateProcessor;
import org.nuxeo.ecm.platform.template.processors.fm.JODReportTemplateProcessor;

/**
 * Helper to find the right {@link TemplateProcessor} given the template type
 * property (wordXMLTemplate, JODTemplate ...).
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ProcessorFactory {

    public static TemplateProcessor getProcessor(String type) {
        if (WordXMLTemplateProcessor.TEMPLATE_TYPE.equals(type)) {
            return new WordXMLTemplateProcessor();
        }
        if (JODReportTemplateProcessor.TEMPLATE_TYPE.equals(type)) {
            return new JODReportTemplateProcessor();
        }
        return null;
    }

}
