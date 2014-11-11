/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.runtime.deployment.preprocessor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.TextTemplate;

/**
 * @author jcarsique
 */
public abstract class ServerConfigurator {

    protected static final Log log = LogFactory.getLog(ServerConfigurator.class);

    protected final ConfigurationGenerator generator;

    public ServerConfigurator(ConfigurationGenerator configurationGenerator) {
        generator = configurationGenerator;
    }

    /**
     * @return true if server configuration files already exist
     */
    abstract boolean isConfigured();

    /**
     * Generate configuration files from templates and given configuration
     * parameters
     *
     * @param config Properties with configuration parameters for template
     *            replacement
     */
    protected void parseAndCopy(Properties config)
            throws IOException {
        // FilenameFilter for excluding "nuxeo.defaults" files from copy
        final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !"nuxeo.defaults".equals(name);
            }
        };
        final TextTemplate templateParser = new TextTemplate(config);
        templateParser.setTrim(true);

        // add included templates directories
        for (File includedTemplate : generator.getIncludedTemplates()) {
            if (includedTemplate.listFiles(filter) != null) {
                for (File in : includedTemplate.listFiles(filter)) {
                    // Retrieve optional target directory if defined
                    String outputDirectoryStr = config.getProperty(includedTemplate.getName()
                            + ".target");
                    File outputDirectory = (outputDirectoryStr != null) ? new File(
                            generator.getNuxeoHome(), outputDirectoryStr)
                            : getOutputDirectory();
                    // copy template(s) directories parsing properties
                    templateParser.processDirectory(in, new File(
                            outputDirectory, in.getName()));
                }
            }
        }
    }

    /**
     * @return output directory for files generation
     */
    protected abstract File getOutputDirectory();

}
