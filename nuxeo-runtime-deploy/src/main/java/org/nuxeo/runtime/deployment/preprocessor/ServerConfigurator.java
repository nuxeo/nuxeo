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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.TextTemplate;

/**
 * @author jcarsique
 *
 */
public abstract class ServerConfigurator {

    protected static final Log log = LogFactory.getLog(ServerConfigurator.class);

    protected ConfigurationGenerator generator;

    /**
     * @param configurationGenerator
     */
    public ServerConfigurator(ConfigurationGenerator configurationGenerator) {
        this.generator=configurationGenerator;
    }

    /**
     * @return true if server configuration files already exist
     */
    abstract boolean isConfigured();

    protected void addDirectories(File[] filesToAdd, List<File> inputDirectories) {
        if (filesToAdd != null) {
            for (File in : filesToAdd) {
                inputDirectories.add(in);
            }
        }
    }

    /**
     * Generate configuration files from templates and given configuration
     * parameters
     *
     * @param config Properties with configuration parameters for template
     *            replacement
     */
    protected void parseAndCopy(Properties config) throws FileNotFoundException,
            IOException {
                TextTemplate templateParser = new TextTemplate(config);

                // copy files to nuxeo.ear
                File outputDirectory = getOutputDirectory();

                // List template directories to copy from (order is: included templates
                // then chosen template)
                List<File> inputDirectories = new ArrayList<File>();
                // FilenameFilter to exclude "nuxeo.defaults" files from copy
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return !"nuxeo.defaults".equals(name);
                    }
                };
                // add included templates directories
                StringTokenizer st = new StringTokenizer(
                        config.getProperty(ConfigurationGenerator.PARAM_INCLUDED_TEMPLATES), ",");
                List<String> includedTemplates = new ArrayList<String>();
                while (st.hasMoreTokens()) {
                    includedTemplates.add(st.nextToken());
                }
                for (String includedTemplate : includedTemplates) {
                    addDirectories(new File(generator.getNuxeoDefaultConf().getParent(),
                            includedTemplate).listFiles(filter), inputDirectories);
                }

                // add chosen template directories
                addDirectories(generator.getTemplateDefaultConf().getParentFile().listFiles(filter),
                        inputDirectories);

                for (File in : inputDirectories) {
                    templateParser.processDirectory(in, new File(outputDirectory,
                            in.getName()));
                }
            }

    /**
     * @return output directory for files generation
     */
    abstract protected File getOutputDirectory();

}
