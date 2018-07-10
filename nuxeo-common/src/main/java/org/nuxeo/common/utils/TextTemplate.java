/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
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
import org.nuxeo.common.codec.Crypto;
import org.nuxeo.common.codec.CryptoProperties;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Text template processing.
 * <p>
 * Copy files or directories replacing parameters matching pattern '${[a-zA-Z_0-9\-\.]+}' with values from a
 * {@link CryptoProperties}.
 * <p>
 * If the value of a variable is encrypted:
 *
 * <pre>
 * setVariable(&quot;var&quot;, Crypto.encrypt(value.getBytes))
 * </pre>
 *
 * then "<code>${var}</code>" will be replaced with:
 * <ul>
 * <li>its decrypted value by default: "<code>value</code>"</li>
 * <li>"<code>${var}</code>" after a call to "<code>setKeepEncryptedAsVar(true)}</code>"
 * </ul>
 * and "<code>${#var}</code>" will always be replaced with its decrypted value.
 * <p>
 * Since 5.7.2, variables can have a default value using syntax ${parameter:=defaultValue}. The default value will be
 * used if parameter is null or unset.
 * <p>
 * Methods {@link #setTextParsingExtensions(String)} and {@link #setFreemarkerParsingExtensions(String)} allow to set
 * the list of files being processed when using {@link #processDirectory(File, File)}, based on their extension; others
 * being simply copied.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @see CryptoProperties
 * @see #setKeepEncryptedAsVar(boolean)
 * @see #setFreemarkerParsingExtensions(String)
 * @see #setTextParsingExtensions(String)
 */
public class TextTemplate {

    private static final Log log = LogFactory.getLog(TextTemplate.class);

    private static final int MAX_RECURSION_LEVEL = 10;

    private static final String PATTERN_GROUP_DECRYPT = "decrypt";

    private static final String PATTERN_GROUP_VAR = "var";

    private static final String PATTERN_GROUP_DEFAULT = "default";

    /**
     * matches variables of the form "${[#]embeddedVar[:=defaultValue]}" but not those starting with "$${"
     */
    private static final Pattern PATTERN = Pattern.compile("(?<!\\$)\\$\\{(?<" + PATTERN_GROUP_DECRYPT + ">#)?" //
            + "(?<" + PATTERN_GROUP_VAR + ">[a-zA-Z_0-9\\-\\.]+)" // embeddedVar
            + "(:=(?<" + PATTERN_GROUP_DEFAULT + ">.*))?\\}"); // defaultValue

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
     * That method is not recursive. It processes the given text only once.
     *
     * @param props CryptoProperties containing the variable values
     * @param text Text to process
     * @return the processed text
     * @since 7.4
     */
    protected String processString(CryptoProperties props, String text) {
        Matcher m = PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String embeddedVar = m.group(PATTERN_GROUP_VAR);
            String value = props.getProperty(embeddedVar, keepEncryptedAsVar);
            if (value == null) {
                value = m.group(PATTERN_GROUP_DEFAULT);
            }
            if (value != null) {
                if (trim) {
                    value = value.trim();
                }
                if (Crypto.isEncrypted(value)) {
                    if (keepEncryptedAsVar && m.group(PATTERN_GROUP_DECRYPT) == null) {
                        value = "${" + embeddedVar + "}";
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
        return sb.toString();
    }

    /**
     * unescape variables
     */
    protected Properties unescape(Properties props) {
        props.replaceAll((k, v) -> unescape((String) v));
        return props;
    }

    protected String unescape(String value) {
        // unescape doubled $ characters, only if in front of a {
        return value.replaceAll("\\$\\$\\{", "\\${");
    }

    private void preprocessVars() {
        processedVars = preprocessVars(vars);
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
                    // newVarsValue == {$[...]$...}
                    assert keepEncryptedAsVar;
                    newVarsValue = "${" + newVarsKey + "}";
                    newVars.put(newVarsKey, newVarsValue);
                    continue;
                }

                String replacementValue = processString(newVars, newVarsValue);
                if (!replacementValue.equals(newVarsValue)) {
                    doneProcessing = false;
                    newVars.put(newVarsKey, replacementValue);
                }
            }
            recursionLevel++;
            // Avoid infinite replacement loops
            if (!doneProcessing && recursionLevel > MAX_RECURSION_LEVEL) {
                log.warn("Detected potential infinite loop when processing the following properties\n" + newVars);
                break;
            }
        }
        return unescape(newVars);
    }

    /**
     * @since 7.4
     */
    public String processText(String text) {
        if (text == null) {
            return null;
        }
        boolean doneProcessing = false;
        int recursionLevel = 0;
        while (!doneProcessing) {
            doneProcessing = true;
            String processedText = processString(vars, text);
            if (!processedText.equals(text)) {
                doneProcessing = false;
                text = processedText;
            }
            recursionLevel++;
            // Avoid infinite replacement loops
            if (!doneProcessing && recursionLevel > MAX_RECURSION_LEVEL) {
                log.warn("Detected potential infinite loop when processing the following text\n" + text);
                break;
            }
        }
        return unescape(text);
    }

    public String processText(InputStream in) throws IOException {
        String text = IOUtils.toString(in, UTF_8);
        return processText(text);
    }

    public void processText(InputStream is, OutputStreamWriter os) throws IOException {
        String text = IOUtils.toString(is, UTF_8);
        text = processText(text);
        os.write(text);
    }

    /**
     * Initialize FreeMarker data model from Java properties.
     * <p>
     * Variables in the form "{@code foo.bar}" (String with dots) are transformed to "{@code foo[bar]}" (arrays).<br>
     * So there will be conflicts if a variable name is equal to the prefix of another variable. For instance, "
     * {@code foo.bar}" and "{@code foo.bar.qux}" will conflict.<br>
     * When a conflict occurs, the conflicting variable is ignored and a warning is logged. The ignored variable will
     * usually be the shortest one (without any contract on this behavior).
     */
    @SuppressWarnings("unchecked")
    public void initFreeMarker() {
        freemarkerConfiguration = new Configuration(Configuration.getVersion());
        preprocessVars();
        freemarkerVars = new HashMap<>();
        Map<String, Object> currentMap;
        String currentString;
        KEYS: for (String key : processedVars.stringPropertyNames()) {
            String value = processedVars.getProperty(key);
            if (value.startsWith("${") && value.endsWith("}")) {
                // crypted variables have to be decrypted in freemarker vars
                value = vars.getProperty(key, false);
            }
            String[] keyparts = key.split("\\.");
            currentMap = freemarkerVars;
            currentString = "";
            for (int i = 0; i < keyparts.length - 1; i++) {
                currentString = currentString + ("".equals(currentString) ? "" : ".") + keyparts[i];
                if (!currentMap.containsKey(keyparts[i])) {
                    Map<String, Object> nextMap = new HashMap<>();
                    currentMap.put(keyparts[i], nextMap);
                    currentMap = nextMap;
                } else if (currentMap.get(keyparts[i]) instanceof Map<?, ?>) {
                    currentMap = (Map<String, Object>) currentMap.get(keyparts[i]);
                } else {
                    // silently ignore known conflicts between Java properties and FreeMarker model
                    if (!key.startsWith("java.vendor") && !key.startsWith("file.encoding")
                            && !key.startsWith("audit.elasticsearch")) {
                        log.warn(String.format("FreeMarker variables: ignored '%s' conflicting with '%s'", key,
                                currentString));
                    }
                    continue KEYS;
                }
            }
            if (!currentMap.containsKey(keyparts[keyparts.length - 1])) {
                currentMap.put(keyparts[keyparts.length - 1], value);
            } else if (!key.startsWith("java.vendor") && !key.startsWith("file.encoding")
                    && !key.startsWith("audit.elasticsearch")) {
                Map<String, Object> currentValue = (Map<String, Object>) currentMap.get(keyparts[keyparts.length - 1]);
                log.warn(String.format("FreeMarker variables: ignored '%2$s' conflicting with '%2$s.%1$s'",
                        currentValue.keySet(), key));
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
     * @see TextTemplate#processText(InputStream, OutputStreamWriter)
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
                    if (filename.equals("." + ext.toLowerCase())) {
                        throw new IOException("Extension only as a filename is not allowed: " + in.getAbsolutePath());
                    }
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
                    try (InputStream is = new FileInputStream(in);
                         OutputStreamWriter os = new OutputStreamWriter(new FileOutputStream(out), "UTF-8")) {
                        processText(is, os);
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
