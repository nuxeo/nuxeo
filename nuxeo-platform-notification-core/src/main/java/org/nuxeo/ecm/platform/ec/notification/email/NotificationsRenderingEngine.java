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
 *     narcis
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ec.notification.email;

import java.io.StringWriter;
import java.io.Writer;

import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingResult;
import org.nuxeo.ecm.platform.rendering.impl.DefaultRenderingResult;
import org.nuxeo.ecm.platform.rendering.template.DocumentRenderingEngine;
import org.nuxeo.ecm.platform.rendering.template.FreemarkerRenderingJob;
import freemarker.template.Configuration;

/**
 * @author <a href="mailto:npaslaru@nuxeo.com">Narcis Paslaru</a>
 */
public class NotificationsRenderingEngine extends DocumentRenderingEngine {

    private final String template;

    public NotificationsRenderingEngine(String template){
        this.template = template;
    }

    @Override
    public Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        cfg.setSharedVariable("htmlEscape", new HtmlEscapeMethod() );
        return cfg;
    }

    @Override
    protected FreemarkerRenderingJob createJob(RenderingContext ctx) {
        return new NotifsRenderingJob("ftl");
    }

    public String getFormatName() {
        // TODO Auto-generated method stub
        return null;
    }

    class NotifsRenderingJob extends DefaultRenderingResult implements FreemarkerRenderingJob {

        private static final long serialVersionUID = -7133062841713259967L;

        final Writer strWriter = new StringWriter();

        NotifsRenderingJob(String formatName) {
            super(formatName);
        }

        @Override
        public Object getOutcome() {
            return strWriter.toString();
        }

        public RenderingResult getResult() {
            return this;
        }

        public String getTemplate() {
            return template;
        }

        public Writer getWriter() {
            return strWriter;
        }
    }

}
