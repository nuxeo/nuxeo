/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.deployment.preprocessor.install.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.common.utils.PathFilter;
import org.nuxeo.common.utils.PathFilterSet;
import org.nuxeo.runtime.deployment.preprocessor.install.Command;
import org.nuxeo.runtime.deployment.preprocessor.install.CommandContext;
import org.nuxeo.runtime.deployment.preprocessor.install.filters.IncludeFilter;

/**
 * @author matic
 * 
 */
public class SeamHotDeployCommand implements Command {

    protected static final Log log = LogFactory.getLog(SeamHotDeployCommand.class);

    public static boolean enabled = false;

    public static PathFilterSet globalFilters = new PathFilterSet();

    private static final String BACKUP_JAR = "~seamhotdeploy";

    private static final String JAR = ".jar";

    private static final String HOTDEPLOY_JAR = "-seamhotdeploy.jar";

    protected final PathFilterSet filters;

    public SeamHotDeployCommand(PathFilterSet fragmentFilters) {
        this.filters = fragmentFilters;
        this.filters.addAll(globalFilters);
    }

    protected static class ColdFilter implements PathFilter {
        protected PathFilter filter;

        public ColdFilter(PathFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean accept(Path path) {
            return !filter.accept(path);
        }

        @Override
        public boolean isExclusive() {
            return filter.isExclusive();
        }

    }

    @Override
    public void exec(CommandContext ctx) throws IOException {
        String originalFilename = ctx.expandVars("${bundle.fileName}");
        if (originalFilename.endsWith(HOTDEPLOY_JAR)) {
            originalFilename = originalFilename.replace(HOTDEPLOY_JAR, JAR);
        }
        Path originalJarPath = new Path(originalFilename);
        Path backupJarPath = new Path(originalFilename.concat(BACKUP_JAR));
        Path seamDevPath = new Path("${war}/WEB-INF/dev");

        // uninstall hot deployment
        File backupJar = new File(ctx.getBaseDir(),backupJarPath.toString());
        if (backupJar.exists()) {
            new DeleteCommand(originalJarPath).exec(ctx);
            new MoveCommand(backupJarPath, originalJarPath).exec(ctx);
        }

        if (!enabled) {
            return;
        }

        // install hot deploy
        loadFilters(new File(ctx.getBaseDir(),originalJarPath.toString()));
        new MoveCommand(originalJarPath, backupJarPath).exec(ctx);
        new UnzipCommand(backupJarPath, seamDevPath, filters).exec(ctx);
        new UnzipCommand(backupJarPath, originalJarPath, new ColdFilter(
                filters)).exec(ctx);
    }

    protected void loadFilters(File file) throws IOException {
        JarFile jf = new JarFile(file);
        ZipEntry ze = jf.getEntry("seam.properties");
        if (ze == null) {
            return;
        }
        InputStream is = jf.getInputStream(ze);
        try {
            loadFilters(filters, is);
        } finally {
            is.close();
        }
    }

    public static void loadFilters(InputStream is) throws IOException {
        loadFilters(globalFilters, is);
    }
    
    protected static void loadFilters(PathFilterSet filters, InputStream is)
            throws IOException {
        Properties props = new Properties();
        props.load(is);
        for (Map.Entry<?, ?> e : props.entrySet()) {
            String enabled = (String) e.getValue();
            if (!Boolean.parseBoolean(enabled)) {
                continue;
            }
            String classname = (String) e.getKey();
            String pattern = classname.replace(".", "/").concat(".class");
            filters.add(new IncludeFilter(pattern));
        }
    }

    @Override
    public String toString(CommandContext ctx) {
        return "seam hot deploy " + ctx.get("bundle.fileName");
    }

}
