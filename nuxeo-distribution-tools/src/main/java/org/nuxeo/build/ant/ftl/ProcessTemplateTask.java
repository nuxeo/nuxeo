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
import java.io.FileWriter;
import java.io.StringWriter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ProcessTemplateTask extends Task {

    public File baseDir;
    public String[] extensions = {"ftl"};
    public boolean removeExtension = true;
    public boolean removeTemplate = true;
    public boolean explicitRemoveTemplate = false;
    public Object input;

    public File toDir;

    public FreemarkerEngine engine;


    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    public void setExtension(String extension) {
        extensions = extension.trim().split("\\s*,\\s*");
    }

    public void setRemoveExtension(boolean removeExtension) {
        this.removeExtension = removeExtension;
    }

    public void setRemoveTemplate(boolean removeTemplate) {
        explicitRemoveTemplate = true;
        this.removeTemplate = removeTemplate;
    }

    public void setTodir(File toDir) {
        this.toDir = toDir;
    }

    @Override
    public void execute() throws BuildException {
        if (engine == null) {
            engine = new FreemarkerEngine();
        }
        engine.setBaseDir(baseDir);
        if (toDir == null) {
            toDir = baseDir;
        } else if (!explicitRemoveTemplate) {
            removeTemplate = false;
        }
        toDir.mkdirs();
        File dir = baseDir;
        String relPath = "";
        processDirectory(dir, relPath);
    }

    public void processDirectory(File dir, String relPath) {
        for (File file : dir.listFiles()) {
            String name = file.getName();
            if (file.isDirectory()) {
                processDirectory(file, relPath+"/"+name);
            } else {
                int p = name.lastIndexOf('.');
                if (p > -1) {
                    String ext = name.substring(p+1);
                    for (int i=0; i<extensions.length; i++) {
                        if (ext.equals(extensions[i])) {
                            processFile(file, relPath+"/"+name, ext);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void processFile(File file, String relPath, String ext) {
        try {
            StringWriter writer = new StringWriter();
            if (input == null) {
                input = engine.createInput(getProject());
            }
            engine.process(input, relPath, writer);
            if (removeExtension) {
                relPath = relPath.substring(0, relPath.length() - ext.length() - 1);
            }
            File f = new File(toDir, relPath);
            f.getParentFile().mkdirs();
            FileWriter out = new FileWriter(f);
            try {
                out.write(writer.getBuffer().toString());
            } finally {
                out.close();
            }
            if (removeTemplate) {
                file.delete();
            }
        } catch (Exception e) {
            throw new BuildException("Failed to process template: " + relPath, e);
        }
    }

}
