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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.template;

import java.io.IOException;

import org.nuxeo.ecm.platform.ec.notification.email.templates.NuxeoTemplatesLoader;
import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Base class for RenderingEngine implementation that will work with freemarker.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public abstract class FreemarkerRenderingEngine implements RenderingEngine {

    protected Configuration cfg;

    /**
     * TODO : It works like this but this default implementation should return just a <code>new Configuration()</code>
     * There should be a class that extends this class and overrides this but that brokes it right now. TODO: write a
     * clear TODO
     */
    public Configuration createConfiguration() {
        Configuration config = new Configuration();
        config.setTemplateLoader(new NuxeoTemplatesLoader());
        config.setDefaultEncoding("UTF-8");
        config.setClassicCompatible(true);
        return config;
    }

    protected abstract FreemarkerRenderingJob createJob(RenderingContext ctx);

    @Override
    public RenderingResult process(RenderingContext ctx) throws RenderingException {
        try {
            if (cfg == null) {
                cfg = createConfiguration();
            }
            FreemarkerRenderingJob job = createJob(ctx);
            cfg.getTemplate(job.getTemplate(), cfg.getDefaultEncoding()).process(ctx, job.getWriter());
            return job.getResult();
        } catch (IOException | TemplateException e) {
            throw new RenderingException("Freemarker processing failed", e);
        }
    }

}
