/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 *
 */

package org.nuxeo.launcher.commons.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Text template processing.
 * <p>
 * Copy files or directories replacing parameters matching pattern
 * '${[a-zA-Z_0-9\-\.]+}' with values from a {@link Map} (deprecated) or a
 * {@link Properties}.
 * <p>
 * Method {@link #setTextParsingExtensions(String)} allow to set list of files
 * being processed when using {@link #processDirectory(File, File)} or #pro,
 * others are simply copied.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TextTemplate {

    private static final Log log = LogFactory.getLog(TextTemplate.class);

    private static final Pattern PATTERN = Pattern.compile("\\$\\{([a-zA-Z_0-9\\-\\.]+)\\}");

    private static final int MAX_RECURSION_LEVEL = 10;

    private final Properties vars;

    private Properties processedVars;

    private boolean trim = false;

    private List<String> plainTextExtensions;

    private List<String> freemarkerExtensions = new ArrayList<String>();

    private Configuration freemarkerConfiguration = null;

    private Map<String, Object> freemarkerVars = null;

    private boolean extensionsContainsDot = false;

    public boolean isTrim() {
        return trim;
    }

    /**
     * Set to true in order to trim invisible characters (spaces) from values.
     */
    public void setTrim(boolean trim) {
        this.trim = trim;
    }

    public TextTemplate() {
        vars = new Properties();
        processedVars = new Properties();
    }

    /**
     * @deprecated prefer use of {@link #TextTemplate(Properties)}
     */
    @Deprecated
    public TextTemplate(Map<String, String> vars) {
        this.vars = new Properties();
        this.vars.putAll(vars);
        processedVars = preprocessVars(this.vars);
    }

    /**
     * @param vars Properties containing keys and values for template processing
     */
    public TextTemplate(Properties vars) {
        this.vars = vars;
        processedVars = preprocessVars(this.vars);
    }

    /**
     * @deprecated prefer use of {@link #getVariables()} then {@link Properties}
     *             .load()
     */
    @Deprecated
    public void setVariables(Map<String, String> vars) {
        this.vars.putAll(vars);
        processedVars = preprocessVars(this.vars);
    }

    public void setVariable(String name, String value) {
        vars.setProperty(name, value);
        processedVars = preprocessVars(vars);
    }

    public String getVariable(String name) {
        return vars.getProperty(name);
    }

    public Properties getVariables() {
        return vars;
    }

    public Properties preprocessVars(Properties unprocessedVars) {
        Properties newVars = new Properties(unprocessedVars);
        boolean doneProcessing = false;
        int recursionLevel = 0;
        while (!doneProcessing) {
            doneProcessing = true;
            @SuppressWarnings("rawtypes")
            Enumeration newVarsEnum = newVars.propertyNames();
            while (newVarsEnum.hasMoreElements()) {
                String newVarsKey = (String) newVarsEnum.nextElement();
                String newVarsValue = newVars.getProperty(newVarsKey);
                Matcher m = PATTERN.matcher(newVarsValue);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String embeddedVar = m.group(1);
                    String value = newVars.getProperty(embeddedVar);
                    if (value != null) {
                        if (trim) {
                            value = value.trim();
                        }
                        String escapedValue = Matcher.quoteReplacement(value);
                        m.appendReplacement(sb, escapedValue);
                    }
                }
                m.appendTail(sb);
                String replacementValue = sb.toString();
                if (!replacementValue.equals(newVarsValue)) {
                    doneProcessing = false;
                    newVars.put(newVarsKey, replacementValue);
                }
            }
            recursionLevel++;
            // Avoid infinite replacement loops
            if ((!doneProcessing) && (recursionLevel > MAX_RECURSION_LEVEL)) {
                break;
            }
        }
        return newVars;
    }

    public String processText(CharSequence text) {
        Matcher m = PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String value = getVariable(var);
            if (value != null) {
                if (trim) {
                    value = value.trim();
                }

                // process again the value if it still contains variable
                // to replace
                String oldValue = value;
                int recursionLevel = 0;
                while (!(value = processText(oldValue)).equals(oldValue)) {
                    oldValue = value;
                    recursionLevel++;
                    // Avoid infinite replacement loops
                    if (recursionLevel > MAX_RECURSION_LEVEL) {
                        break;
                    }
                }

                // Allow use of backslash and dollars characters
                String valueL = Matcher.quoteReplacement(value);
                m.appendReplacement(sb, valueL);
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String processText(InputStream in) throws IOException {
        String text = IOUtils.toString(in, "UTF-8");
        return processText(text);
    }

    public void processText(InputStream is, OutputStream os) throws IOException {
        String text = IOUtils.toString(is, "UTF-8");
        text = processText(text);
        os.write(text.getBytes("UTF-8"));
    }

    @SuppressWarnings("unchecked")
    public void initFreeMarker() {
        // Initialize FreeMarker
        freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setObjectWrapper(new DefaultObjectWrapper());
        // Initialize data model
        freemarkerVars = new HashMap<String, Object>();
        @SuppressWarnings("rawtypes")
        Enumeration processedEnum = processedVars.propertyNames();
        Map<String, Object> currentMap;
        String currentString;
        while (processedEnum.hasMoreElements()) {
            String key = (String) processedEnum.nextElement();
            String value = processedVars.getProperty(key);
            String[] keyparts = key.split("\\.");
            currentMap = freemarkerVars;
            currentString = "";
            boolean setKeyVal = true;
            for (int i = 0; i < (keyparts.length - 1); i++) {
                currentString = currentString
                        + (currentString.equals("") ? "" : ".") + keyparts[i];
                if (!currentMap.containsKey(keyparts[i])) {
                    Map<String, Object> nextMap = new HashMap<String, Object>();
                    currentMap.put(keyparts[i], nextMap);
                    currentMap = nextMap;
                } else {
                    if (currentMap.get(keyparts[i]) instanceof Map<?, ?>) {
                        currentMap = (Map<String, Object>) currentMap.get(keyparts[i]);
                    } else {
                        // silently ignore known conflicts in java properties
                        if (!key.startsWith("java.vendor")) {
                            log.warn("FreeMarker templates: "
                                    + currentString
                                    + " is already defined - "
                                    + key
                                    + " will not be available in the data model.");
                        }
                        setKeyVal = false;
                        break;
                    }
                }
            }
            if (setKeyVal) {
                currentMap.put(keyparts[keyparts.length - 1], value);
            }
        }
    }

    public void processFreemarker(File in, File out) throws IOException,
            TemplateException {
        if (freemarkerConfiguration == null) {
            initFreeMarker();
        }
        freemarkerConfiguration.setDirectoryForTemplateLoading(in.getParentFile());
        Template nxtpl = freemarkerConfiguration.getTemplate(in.getName());
        Writer outWriter = new FileWriter(out);
        nxtpl.process(freemarkerVars, outWriter);
        outWriter.close();
    }

    /**
     * Recursive call {@link #process(InputStream, OutputStream, boolean)} on
     * each file from "in" directory to "out" directory.
     *
     * @param in Directory to read files from
     * @param out Directory to write files to
     * @return copied files list
     */
    public List<String> processDirectory(File in, File out)
            throws FileNotFoundException, IOException, TemplateException {
        List<String> newFiles = new ArrayList<String>();
        if (in.isFile()) {
            if (out.isDirectory()) {
                out = new File(out, in.getName());
            }
            if (!out.getParentFile().exists()) {
                out.getParentFile().mkdirs();
            }

            boolean processAsText = false;
            boolean processAsFreemarker = false;
            String freemarkerExtension = null;
            if (!extensionsContainsDot) {
                int extIndex = in.getName().lastIndexOf('.');
                String extension = extIndex == -1 ? ""
                        : in.getName().substring(extIndex + 1).toLowerCase();
                processAsText = plainTextExtensions == null
                        || plainTextExtensions.contains(extension);
                if (freemarkerExtensions.contains(extension)) {
                    processAsFreemarker = true;
                    freemarkerExtension = extension;
                }

            } else {
                // Check for each extension if it matches end of filename
                String filename = in.getName().toLowerCase();
                for (String ext : plainTextExtensions) {
                    if (filename.endsWith(ext)) {
                        processAsText = true;
                        processAsFreemarker = false;
                        freemarkerExtension = ext;
                        break;
                    }
                }
                for (String ext : freemarkerExtensions) {
                    if (filename.endsWith(ext)) {
                        processAsText = false;
                        processAsFreemarker = true;
                        break;
                    }
                }
            }

            if (processAsFreemarker) {
                out = new File(out.getCanonicalPath().replaceAll(
                        "\\.*" + Pattern.quote(freemarkerExtension) + "$", ""));
            }
            // Backup existing file if not already done
            if (out.exists()) {
                File backup = new File(out.getPath() + ".bak");
                if (!backup.exists()) {
                    log.debug("Backup " + out);
                    FileUtils.copyFile(out, backup);
                    newFiles.add(backup.getPath());
                }
            } else {
                newFiles.add(out.getPath());
            }
            if (processAsFreemarker) {
                processFreemarker(in, out);
            } else if (processAsText) {
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = new FileInputStream(in);
                    os = new FileOutputStream(out);
                    processText(is, os);
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                }
            } else {
                FileUtils.copyFile(in, out);
            }
        } else if (in.isDirectory()) {
            if (!out.exists()) {
                // allow renaming destination directory
                out.mkdirs();
            } else if (!out.getName().equals(in.getName())) {
                // allow copy over existing arborescence
                out = new File(out, in.getName());
                out.mkdir();
            }
            for (File file : in.listFiles()) {
                newFiles.addAll(processDirectory(file, out));
            }
        }
        return newFiles;
    }

    /**
     * @param extensionsList comma-separated list of files extensions to parse
     */
    public void setTextParsingExtensions(String extensionsList) {
        StringTokenizer st = new StringTokenizer(extensionsList, ",");
        plainTextExtensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String extension = st.nextToken();
            plainTextExtensions.add(extension);
            if (!extensionsContainsDot && extension.contains(".")) {
                extensionsContainsDot = true;
            }
        }
    }

    public void setFreemarkerParsingExtensions(String extensionsList) {
        StringTokenizer st = new StringTokenizer(extensionsList, ",");
        freemarkerExtensions = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String extension = st.nextToken();
            freemarkerExtensions.add(extension);
            if (!extensionsContainsDot && extension.contains(".")) {
                extensionsContainsDot = true;
            }
        }
    }

}
