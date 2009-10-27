/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.build.ant.ftl;

import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.nuxeo.build.maven.MavenClientFactory;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FreemarkerEngine {

    protected Configuration cfg;

    public FreemarkerEngine() {
        this.cfg = getDefaultConfiguration();
    }


    public void setBaseDir(File baseDir) throws BuildException {
        try {
            cfg.setTemplateLoader(new FileTemplateLoader(baseDir));
        } catch (Exception e) {
            throw new BuildException("Failed toc reate freemarker configuration", e);
        }
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    protected Configuration getDefaultConfiguration() {
        Configuration cfg = new Configuration();
        cfg.setWhitespaceStripping(true);
        cfg.setLocalizedLookup(false);
        cfg.setClassicCompatible(true);
        return cfg;
    }

    @SuppressWarnings("unchecked")
    public Object createInput(Project project) {
        Map<String, Object> root = new HashMap<String, Object>();
        root.putAll(project.getProperties());
        root.put("ant", project.getProperties());
        root.put("system", System.getProperties());
        root.put("profiles", MavenClientFactory.getInstance().getAntProfileManager());
        root.put("graph", MavenClientFactory.getInstance().getGraph());
        return root;
    }

    public Template getTemplate(String name) {
        try {
            return cfg.getTemplate(name);
        } catch (Exception e) {
            throw new BuildException("Failed to create template "+name, e);
        }
    }

    public void process(Project project, String name, Writer writer) {
        process(createInput(project), name, writer);
    }

    public void process(Object input, String name, Writer writer) {
        try {
            Template tpl = cfg.getTemplate(name);
            tpl.process(input, writer);
        } catch (Exception e) {
            throw new BuildException("Failed to process template "+name, e);
        }
    }

}
