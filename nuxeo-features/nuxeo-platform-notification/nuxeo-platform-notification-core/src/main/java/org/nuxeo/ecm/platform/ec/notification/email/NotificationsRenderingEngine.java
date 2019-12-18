/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    public NotificationsRenderingEngine(String template) {
        this.template = template;
    }

    @Override
    public Configuration createConfiguration() {
        Configuration cfg = super.createConfiguration();
        cfg.setSharedVariable("htmlEscape", new HtmlEscapeMethod());
        return cfg;
    }

    @Override
    protected FreemarkerRenderingJob createJob(RenderingContext ctx) {
        return new NotifsRenderingJob("ftl");
    }

    @Override
    public String getFormatName() {
        return template;
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

        @Override
        public RenderingResult getResult() {
            return this;
        }

        @Override
        public String getTemplate() {
            return template;
        }

        @Override
        public Writer getWriter() {
            return strWriter;
        }
    }

}
