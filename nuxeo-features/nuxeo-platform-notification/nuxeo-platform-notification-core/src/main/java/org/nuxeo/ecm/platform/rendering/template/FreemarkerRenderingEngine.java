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
 *     dragos
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.template;

import org.nuxeo.ecm.platform.ec.notification.email.templates.NuxeoTemplatesLoader;
import org.nuxeo.ecm.platform.rendering.RenderingContext;
import org.nuxeo.ecm.platform.rendering.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.RenderingException;
import org.nuxeo.ecm.platform.rendering.RenderingResult;

import freemarker.template.Configuration;

/**
 * Base class for RenderingEngine implementation that will work with freemarker.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public abstract class FreemarkerRenderingEngine implements RenderingEngine {

    protected Configuration cfg;

    /**
     * TODO : It works like this but this default implementation should return
     * just a <code>new Configuration()</code> There should be a class that
     * extends this class and overrides this but that brokes it right now.
     *
     * TODO: write a clear TODO
     */
    public Configuration createConfiguration() throws Exception {
        Configuration config = new Configuration();
        config.setTemplateLoader(new NuxeoTemplatesLoader());
        config.setDefaultEncoding("UTF-8");
        return config;
    }

    protected abstract FreemarkerRenderingJob createJob(RenderingContext ctx) ;

    public RenderingResult process(RenderingContext ctx)
            throws RenderingException {
        try {
            if (cfg == null) {
                cfg = createConfiguration();
            }
            FreemarkerRenderingJob job = createJob(ctx);
            cfg.getTemplate(job.getTemplate(), cfg.getDefaultEncoding()).process(
                    ctx, job.getWriter());
            return job.getResult();
        } catch (RenderingException e) {
            throw e;
        } catch (Exception e) {
            throw new RenderingException("Freemarker processing failed", e);
        }
    }

}
