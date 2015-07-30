/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     bstefanescu, jcarsique
 *     Anahide Tchertchian
 *
 */

package org.nuxeo.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Text template processing.
 * <p>
 * Copy files or directories replacing parameters matching pattern '${[a-zA-Z_0-9\-\.]+}' with values from a {@link Map}
 * (deprecated) or a {@link Properties}.
 * <p>
 * Since 5.7.2, variables can have a default value using syntax ${parameter:=defaultValue}. The default value will be
 * used if parameter is null or unset.
 * <p>
 * Method {@link #setTextParsingExtensions(String)} allow to set the list of files being processed when using
 * {@link #processDirectory(File, File)}, based on their extension; others being simply copied.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TextTemplate {

    private static final Log log = LogFactory.getLog(TextTemplate.class);

    private static final int MAX_RECURSION_LEVEL = 10;

    private static final Pattern PATTERN = Pattern.compile("(?<!\\$)\\$\\{([a-zA-Z_0-9\\-\\.]+)(:=(.*))?\\}");

    private final CryptoProperties vars;

    private Properties processedVars;

    private boolean trim = false;

    private List<String> plainTextExtensions;

    private List<String> freemarkerExtensions = new ArrayList<>();

    private Configuration freemarkerConfiguration = null;

    private Map<String, Object> freemarkerVars = null;

    private boolean keepEncryptedAsVar;

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
        vars = new CryptoProperties();
    }

    /**
     * {@link #TextTemplate(Properties)} provides an additional default values behavior
     *
     * @see #TextTemplate(Properties)
     */
    public TextTemplate(Map<String, String> vars) {
        this.vars = new CryptoProperties();
        this.vars.putAll(vars);
    }

    /**
     * @param vars Properties containing keys and values for template processing
     */
    public TextTemplate(Properties vars) {
        if (vars instanceof CryptoProperties) {
            this.vars = (CryptoProperties) vars;
        } else {
            this.vars = new CryptoProperties(vars);
        }
    }

    public void setVariables(Map<String, String> vars) {
        this.vars.putAll(vars);
        freemarkerConfiguration = null;
    }

    /**
     * If adding multiple variables, prefer use of {@link #setVariables(Map)}
     */
    public void setVariable(String name, String value) {
        vars.setProperty(name, value);
        freemarkerConfiguration = null;
    }

    public String getVariable(String name) {
        return vars.getProperty(name, keepEncryptedAsVar);
    }

    public Properties getVariables() {
        return vars;
    }

    /**
     * @deprecated Since 7.4. Use {@link #processText(CharSequence)} instead.
     */
    @Deprecated
    public String process(CharSequence text) {
        return processText(text);
    }

    /**
     * @deprecated Since 7.4. Use {@link #processText(InputStream)} instead.
     */
    @Deprecated
    public String process(InputStream in) throws IOException {
        return processText(in);
    }

    /**
     * @deprecated Since 7.4. Use {@link #processText(InputStream, OutputStream)} instead.
     */
    @Deprecated
    public void process(InputStream in, OutputStream out) throws IOException {
        processText(in, out);
    }

    /**
     * @param processText if true, text is processed for parameters replacement
     * @deprecated Since 7.4. Use {@link #processText(InputStream, OutputStream)} (if {@code processText}) or
     *             {@link IOUtils#copy(InputStream, OutputStream)}
     */
    @Deprecated
    public void process(InputStream is, OutputStream os, boolean processText) throws IOException {
        if (processText) {
            String text = IOUtils.toString(is, Charsets.UTF_8);
            text = processText(text);
            os.write(text.getBytes());
        } else {
            IOUtils.copy(is, os);
        }
    }

    private void preprocessVars() {
        processedVars = preprocessVars(vars);
        freemarkerConfiguration = null;
    }

    public Properties preprocessVars(Properties unprocessedVars) {
        CryptoProperties newVars = new CryptoProperties(unprocessedVars);
        boolean doneProcessing = false;
        int recursionLevel = 0;
        while (!doneProcessing) {
            doneProcessing = true;
            for (String newVarsKey : newVars.stringPropertyNames()) {
                String newVarsValue = newVars.getProperty(newVarsKey, keepEncryptedAsVar);
                if (newVarsValue == null) {
                    continue;
                }
                if (Crypto.isEncrypted(newVarsValue)) {
                    if (keepEncryptedAsVar) {
                        newVarsValue = "${" + newVarsKey + "}";
                    } else {
                        newVarsValue = new String(newVars.getCrypto().decrypt(newVarsValue));
                    }
                    newVars.put(newVarsKey, newVarsValue);
                    continue;
                }
                Matcher m = PATTERN.matcher(newVarsValue);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String embeddedVar = m.group(1);
                    String value = newVars.getProperty(embeddedVar, keepEncryptedAsVar);
                    if (m.groupCount() >= 3 && value == null) {
                        value = m.group(3);
                    }
                    if (value != null) {
                        if (trim) {
                            value = value.trim();
                        }
                        if (Crypto.isEncrypted(value)) {
                            if (keepEncryptedAsVar) {
                                value = "${" + embeddedVar + "}";
                            } else {
                                value = new String(vars.getCrypto().decrypt(value));
                            }
                        }

                        value = Matcher.quoteReplacement(value);
                        m.appendReplacement(sb, value);
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
        return unescape(newVars);
    }

    protected Properties unescape(Properties props) {
        // unescape variables
        for (Object key : props.keySet()) {
            props.put(key, unescape((String) props.get(key)));
        }
        return props;
    }

    protected String unescape(String value) {
        return value.replaceAll("(?<!\\{)\\$\\$", "\\$");
    }

    public String processText(CharSequence text) {
        if (text == null) {
            return null;
        }
        Matcher m = PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String var = m.group(1);
            String value = vars.getProperty(var, keepEncryptedAsVar);
            if (m.groupCount() >= 3 && value == null) {
                value = m.group(3);
            }
            if (value != null) {
                if (trim) {
                    value = value.trim();
                }

                // process again the value if it still contains variable to replace
                String oldValue = value;
                int recursionLevel = 0;
                while (!(value = processText(oldValue)).equals(oldValue)) {
                    oldValue = value;
                    recursionLevel++;
                    // Avoid infinite replacement loops
                    if (recursionLevel > MAX_RECURSION_LEVEL) {
                        log.warn(String.format("Detected potential infinite loop on variable processing\n"
                                + "Text: %s\nVariable: %s\nValue %d: %s\nValue %d: %s", text, var, MAX_RECURSION_LEVEL,
                                oldValue, recursionLevel, value));
                        break;
                    }
                }
                if (Crypto.isEncrypted(value)) {
                    if (keepEncryptedAsVar) {
                        value = "${" + var + "}";
                    } else {
                        value = new String(vars.getCrypto().decrypt(value));
                    }
                }

                // Allow use of backslash and dollars characters
                value = Matcher.quoteReplacement(value);
                m.appendReplacement(sb, value);
            }
        }
        m.appendTail(sb);
        return unescape(sb.toString());
    }

    public String processText(InputStream in) throws IOException {
        String text = IOUtils.toString(in, Charsets.UTF_8);
        return processText(text);
    }

    public void processText(InputStream is, OutputStream os) throws IOException {
        String text = IOUtils.toString(is, Charsets.UTF_8);
        text = processText(text);
        os.write(text.getBytes(Charsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public void initFreeMarker() {
        // Initialize FreeMarker
        freemarkerConfiguration = new Configuration(Configuration.getVersion());
        // Initialize data model
        preprocessVars();
        freemarkerVars = new HashMap<>();
        Map<String, Object> currentMap;
        String currentString;
        for (String key : processedVars.stringPropertyNames()) {
            String value = processedVars.getProperty(key);
            String[] keyparts = key.split("\\.");
            currentMap = freemarkerVars;
            currentString = "";
            boolean setKeyVal = true;
            for (int i = 0; i < (keyparts.length - 1); i++) {
                currentString = currentString + (currentString.equals("") ? "" : ".") + keyparts[i];
                if (!currentMap.containsKey(keyparts[i])) {
                    Map<String, Object> nextMap = new HashMap<>();
                    currentMap.put(keyparts[i], nextMap);
                    currentMap = nextMap;
                } else {
                    if (currentMap.get(keyparts[i]) instanceof Map<?, ?>) {
                        currentMap = (Map<String, Object>) currentMap.get(keyparts[i]);
                    } else {
                        // silently ignore known conflicts in java properties
                        if (!key.startsWith("java.vendor")) {
                            log.warn("FreeMarker templates: " + currentString + " is already defined - " + key
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

    public void processFreemarker(File in, File out) throws IOException, TemplateException {
        if (freemarkerConfiguration == null) {
            initFreeMarker();
        }
        freemarkerConfiguration.setDirectoryForTemplateLoading(in.getParentFile());
        Template nxtpl = freemarkerConfiguration.getTemplate(in.getName());
        try (Writer writer = new EscapeVariableFilter(new FileWriter(out))) {
            nxtpl.process(freemarkerVars, writer);
        }
    }

    protected static class EscapeVariableFilter extends FilterWriter {

        protected static final int DOLLAR_SIGN = "$".codePointAt(0);

        protected int last;

        public EscapeVariableFilter(Writer out) {
            super(out);
        }

        public @Override void write(int b) throws IOException {
            if (b == DOLLAR_SIGN && last == DOLLAR_SIGN) {
                return;
            }
            last = b;
            super.write(b);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for (int i = 0; i < len; ++i) {
                write(cbuf[off + i]);
            }
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            write(cbuf, 0, cbuf.length);
        }

    }

    /**
     * Recursively process each file from "in" directory to "out" directory.
     *
     * @param in Directory to read files from
     * @param out Directory to write files to
     * @return copied files list
     * @see TextTemplate#processText(InputStream, OutputStream)
     * @see TextTemplate#processFreemarker(File, File)
     */
    public List<String> processDirectory(File in, File out) throws FileNotFoundException, IOException,
            TemplateException {
        List<String> newFiles = new ArrayList<>();
        if (in.isFile()) {
            if (out.isDirectory()) {
                out = new File(out, in.getName());
            }
            if (!out.getParentFile().exists()) {
                out.getParentFile().mkdirs();
            }

            boolean processAsText = false;
            boolean processAsFreemarker = false;
            // Check for each extension if it matches end of filename
            String filename = in.getName().toLowerCase();
            for (String ext : freemarkerExtensions) {
                if (filename.endsWith(ext)) {
                    processAsFreemarker = true;
                    out = new File(out.getCanonicalPath().replaceAll("\\.*" + Pattern.quote(ext) + "$", ""));
                    break;
                }
            }
            if (!processAsFreemarker) {
                for (String ext : plainTextExtensions) {
                    if (filename.endsWith(ext)) {
                        processAsText = true;
                        break;
                    }
                }
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
            try {
                if (processAsFreemarker) {
                    log.debug("Process as FreeMarker " + in.getPath());
                    processFreemarker(in, out);
                } else if (processAsText) {
                    log.debug("Process as Text " + in.getPath());
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
                    log.debug("Process as copy " + in.getPath());
                    FileUtils.copyFile(in, out);
                }
            } catch (IOException | TemplateException e) {
                log.error("Failure on " + in.getPath());
                throw e;
            }
        } else if (in.isDirectory()) {
            if (!out.exists()) {
                // allow renaming destination directory
                out.mkdirs();
            } else if (!out.getName().equals(in.getName())) {
                // allow copy over existing hierarchy
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
     * @deprecated Since 7.4. Use {@link #setTextParsingExtensions(String)} instead.
     * @see #setTextParsingExtensions(String)
     * @see #setFreemarkerParsingExtensions(String)
     */
    @Deprecated
    public void setParsingExtensions(String extensionsList) {
        setTextParsingExtensions(extensionsList);
    }

    /**
     * @param extensionsList comma-separated list of files extensions to parse
     */
    public void setTextParsingExtensions(String extensionsList) {
        StringTokenizer st = new StringTokenizer(extensionsList, ",");
        plainTextExtensions = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String extension = st.nextToken().toLowerCase();
            plainTextExtensions.add(extension);
        }
    }

    public void setFreemarkerParsingExtensions(String extensionsList) {
        StringTokenizer st = new StringTokenizer(extensionsList, ",");
        freemarkerExtensions = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String extension = st.nextToken().toLowerCase();
            freemarkerExtensions.add(extension);
        }
    }

    /**
     * Whether to replace or not the variables which value is encrypted.
     *
     * @param keepEncryptedAsVar if {@code true}, the variables which value is encrypted won't be expanded
     * @since 7.4
     */
    public void setKeepEncryptedAsVar(boolean keepEncryptedAsVar) {
        if (this.keepEncryptedAsVar != keepEncryptedAsVar) {
            this.keepEncryptedAsVar = keepEncryptedAsVar;
            freemarkerConfiguration = null;
        }
    }

}
