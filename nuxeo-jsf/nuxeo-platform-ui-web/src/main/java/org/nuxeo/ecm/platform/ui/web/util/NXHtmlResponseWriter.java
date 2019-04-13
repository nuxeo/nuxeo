/*
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Portions Copyrighted 2013 Nuxeo
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.sun.faces.RIConstants;
import com.sun.faces.config.WebConfiguration;
import com.sun.faces.io.FastStringWriter;
import com.sun.faces.renderkit.html_basic.HtmlResponseWriter;
import com.sun.faces.util.HtmlUtils;
import com.sun.faces.util.MessageUtils;

/**
 * CSV specific response writer copied pasted from com.sun.faces.renderkit.html_basic.HtmlResponseWriter without the
 * HTML encode part.
 *
 * @since 5.9.1, 5.8-HF01
 */
public class NXHtmlResponseWriter extends ResponseWriter {

    // Content Type for this Writer.
    //
    private String contentType = "text/html";

    // Character encoding of that Writer - this may be null
    // if the encoding isn't known.
    //
    private String encoding = null;

    // Writer to use for output;
    //
    private Writer writer = null;

    // True when we need to close a start tag
    //
    private boolean closeStart;

    // Configuration flag regarding disableUnicodeEscaping
    //
    private WebConfiguration.DisableUnicodeEscaping disableUnicodeEscaping;

    // Flag to escape Unicode
    //
    private boolean escapeUnicode;

    // Flag to escape ISO-8859-1 codes
    //
    private boolean escapeIso;

    // flag to indicate we're writing a CDATA section
    private boolean writingCdata;

    // flat to indicate the current element is CDATA
    private boolean isCdata;

    // flag to indicate that we're writing a 'script' or 'style' element
    private boolean isScript;

    // flag to indicate that we're writing a 'style' element
    private boolean isStyle;

    // flag to indicate that we're writing a 'src' attribute as part of
    // 'script' or 'style' element
    private boolean scriptOrStyleSrc;

    // flag to indicate if the content type is Xhtml
    private boolean isXhtml;

    // HtmlResponseWriter to use when buffering is required
    private Writer origWriter;

    // Keep one instance of the script buffer per Writer
    private FastStringWriter scriptBuffer;

    // Keep one instance of attributesBuffer to buffer the writting
    // of all attributes for a particular element to reducr the number
    // of writes
    private FastStringWriter attributesBuffer;

    // Enables hiding of inlined script and style
    // elements from old browsers
    private Boolean isScriptHidingEnabled;

    // Enables scripts to be included in attribute values
    private Boolean isScriptInAttributeValueEnabled;

    // Internal buffer used when outputting properly escaped information
    // using HtmlUtils class.
    //
    private char[] buffer = new char[1028];

    // Internal buffer for to store the result of String.getChars() for
    // values passed to the writer as String to reduce the overhead
    // of String.charAt(). This buffer will be grown, if necessary, to
    // accomodate larger values.
    private char[] textBuffer = new char[128];

    static final Pattern CDATA_START_SLASH_SLASH;

    static final Pattern CDATA_END_SLASH_SLASH;

    static final Pattern CDATA_START_SLASH_STAR;

    static final Pattern CDATA_END_SLASH_STAR;

    static {
        // At the beginning of a line, match // followed by any amount of
        // whitespace, followed by <![CDATA[
        CDATA_START_SLASH_SLASH = Pattern.compile("^//\\s*\\Q<![CDATA[\\E");

        // At the end of a line, match // followed by any amout of whitespace,
        // followed by ]]>
        CDATA_END_SLASH_SLASH = Pattern.compile("//\\s*\\Q]]>\\E$");

        // At the beginning of a line, match /* followed by any amout of
        // whitespace, followed by <![CDATA[, followed by any amount of
        // whitespace,
        // followed by */
        CDATA_START_SLASH_STAR = Pattern.compile("^/\\*\\s*\\Q<![CDATA[\\E\\s*\\*/");

        // At the end of a line, match /* followed by any amount of whitespace,
        // followed by ]]> followed by any amount of whitespace, followed by */
        CDATA_END_SLASH_STAR = Pattern.compile("/\\*\\s*\\Q]]>\\E\\s*\\*/$");

    }

    // ------------------------------------------------------------
    // Constructors

    /**
     * Constructor sets the <code>ResponseWriter</code> and encoding, and enables script hiding by default.
     *
     * @param writer the <code>ResponseWriter</code>
     * @param contentType the content type.
     * @param encoding the character encoding.
     * @throws javax.faces.FacesException the encoding is not recognized.
     */
    public NXHtmlResponseWriter(Writer writer, String contentType, String encoding) throws FacesException {
        this(writer, contentType, encoding, null, null, null);
    }

    /**
     * <p>
     * Constructor sets the <code>ResponseWriter</code> and encoding.
     * </p>
     * <p>
     * The argument configPrefs is a map of configurable prefs that affect this instance's behavior. Supported keys are:
     * </p>
     * <p>
     * BooleanWebContextInitParameter.EnableJSStyleHiding: <code>true</code> if the writer should attempt to hide JS
     * from older browsers
     * </p>
     *
     * @param writer the <code>ResponseWriter</code>
     * @param contentType the content type.
     * @param encoding the character encoding.
     * @throws javax.faces.FacesException the encoding is not recognized.
     */
    public NXHtmlResponseWriter(Writer writer, String contentType, String encoding, Boolean isScriptHidingEnabled,
            Boolean isScriptInAttributeValueEnabled, WebConfiguration.DisableUnicodeEscaping disableUnicodeEscaping)
            throws FacesException {

        this.writer = writer;

        if (null != contentType) {
            this.contentType = contentType;
        }

        this.encoding = encoding;

        // init those configuration parameters not yet initialized
        WebConfiguration webConfig = null;
        if (isScriptHidingEnabled == null) {
            webConfig = getWebConfiguration(webConfig);
            isScriptHidingEnabled = (null == webConfig) ? WebConfiguration.BooleanWebContextInitParameter.EnableJSStyleHiding.getDefaultValue()
                    : webConfig.isOptionEnabled(WebConfiguration.BooleanWebContextInitParameter.EnableJSStyleHiding);
        }

        if (isScriptInAttributeValueEnabled == null) {
            webConfig = getWebConfiguration(webConfig);
            isScriptInAttributeValueEnabled = (null == webConfig) ? WebConfiguration.BooleanWebContextInitParameter.EnableScriptInAttributeValue.getDefaultValue()
                    : webConfig.isOptionEnabled(WebConfiguration.BooleanWebContextInitParameter.EnableScriptInAttributeValue);
        }

        if (disableUnicodeEscaping == null) {
            webConfig = getWebConfiguration(webConfig);
            disableUnicodeEscaping = WebConfiguration.DisableUnicodeEscaping.getByValue((null == webConfig) ? WebConfiguration.WebContextInitParameter.DisableUnicodeEscaping.getDefaultValue()
                    : webConfig.getOptionValue(WebConfiguration.WebContextInitParameter.DisableUnicodeEscaping));
            if (disableUnicodeEscaping == null) {
                disableUnicodeEscaping = WebConfiguration.DisableUnicodeEscaping.False;
            }
        }

        // and store them for later use
        this.isScriptHidingEnabled = isScriptHidingEnabled;
        this.isScriptInAttributeValueEnabled = isScriptInAttributeValueEnabled;
        this.disableUnicodeEscaping = disableUnicodeEscaping;

        attributesBuffer = new FastStringWriter(128);

        // Check the character encoding
        if (!HtmlUtils.validateEncoding(encoding)) {
            throw new IllegalArgumentException(
                    MessageUtils.getExceptionMessageString(MessageUtils.ENCODING_ERROR_MESSAGE_ID));
        }

        String charsetName = encoding.toUpperCase();

        switch (disableUnicodeEscaping) {
        case True:
            // html escape noting (except the dangerous characters like "<>'"
            // etc
            escapeUnicode = false;
            escapeIso = false;
            break;
        case False:
            // html escape any non-ascii character
            escapeUnicode = true;
            escapeIso = true;
            break;
        case Auto:
            // is stream capable of rendering unicode, do not escape
            escapeUnicode = !HtmlUtils.isUTFencoding(charsetName);
            // is stream capable of rendering unicode or iso-8859-1, do not
            // escape
            escapeIso = !HtmlUtils.isISO8859_1encoding(charsetName) && !HtmlUtils.isUTFencoding(charsetName);
            break;
        }
    }

    private WebConfiguration getWebConfiguration(WebConfiguration webConfig) {
        if (webConfig != null) {
            return webConfig;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        if (null != context) {
            ExternalContext extContext = context.getExternalContext();
            if (null != extContext) {
                webConfig = WebConfiguration.getInstance(extContext);
            }
        }
        return webConfig;
    }

    // -------------------------------------------------- Methods From
    // Closeable

    /** Methods From <code>java.io.Writer</code> */

    @Override
    public void close() throws IOException {

        closeStartIfNecessary();
        writer.close();

    }

    // -------------------------------------------------- Methods From
    // Flushable

    /**
     * Flush any buffered output to the contained writer.
     *
     * @throws IOException if an input/output error occurs.
     */
    @Override
    public void flush() throws IOException {

        // NOTE: Internal buffer's contents (the ivar "buffer") is
        // written to the contained writer in the HtmlUtils class - see
        // HtmlUtils.flushBuffer method; Buffering is done during
        // writeAttribute/writeText - otherwise, output is written
        // directly to the writer (ex: writer.write(....)..
        //
        // close any previously started element, if necessary
        closeStartIfNecessary();

    }

    // ---------------------------------------------------------- Public
    // Methods

    /** @return the content type such as "text/html" for this ResponseWriter. */
    @Override
    public String getContentType() {

        return contentType;

    }

    /**
     * <p>
     * Create a new instance of this <code>ResponseWriter</code> using a different <code>Writer</code>.
     *
     * @param writer The <code>Writer</code> that will be used to create another <code>ResponseWriter</code>.
     */
    @Override
    public ResponseWriter cloneWithWriter(Writer writer) {

        try {
            return new HtmlResponseWriter(writer, getContentType(), getCharacterEncoding(), isScriptHidingEnabled,
                    isScriptInAttributeValueEnabled, disableUnicodeEscaping, false);
        } catch (FacesException e) {
            // This should never happen
            throw new IllegalStateException();
        }

    }

    /** Output the text for the end of a document. */
    @Override
    public void endDocument() throws IOException {

        writer.flush();

    }

    /**
     * <p>
     * Write the end of an element. This method will first close any open element created by a call to
     * <code>startElement()</code>.
     *
     * @param name Name of the element to be ended
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    @Override
    public void endElement(String name) throws IOException {

        if (name == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "name"));
        }

        isXhtml = getContentType().equals(RIConstants.XHTML_CONTENT_TYPE);

        if (isScriptOrStyle(name) && !scriptOrStyleSrc && writer instanceof FastStringWriter) {
            String result = ((FastStringWriter) writer).getBuffer().toString();
            writer = origWriter;

            if (result != null) {
                String trim = result.trim();
                if (isXhtml) {
                    if (isScript) {
                        Matcher cdataStartSlashSlash = CDATA_START_SLASH_SLASH.matcher(trim), cdataEndSlashSlash = CDATA_END_SLASH_SLASH.matcher(trim), cdataStartSlashStar = CDATA_START_SLASH_STAR.matcher(trim), cdataEndSlashStar = CDATA_END_SLASH_STAR.matcher(trim);
                        int trimLen = trim.length(), start, end;
                        // case 1 start is // end is //
                        if (cdataStartSlashSlash.find() && cdataEndSlashSlash.find()) {
                            start = cdataStartSlashSlash.end() - cdataStartSlashSlash.start();
                            end = trimLen - (cdataEndSlashSlash.end() - cdataEndSlashSlash.start());
                            writer.write(trim.substring(start, end));
                        }
                        // case 2 start is // end is /* */
                        else if ((null != cdataStartSlashSlash.reset() && cdataStartSlashSlash.find())
                                && cdataEndSlashStar.find()) {
                            start = cdataStartSlashSlash.end() - cdataStartSlashSlash.start();
                            end = trimLen - (cdataEndSlashStar.end() - cdataEndSlashStar.start());
                            writer.write(trim.substring(start, end));
                        }
                        // case 3 start is /* */ end is /* */
                        else if (cdataStartSlashStar.find()
                                && (null != cdataEndSlashStar.reset() && cdataEndSlashStar.find())) {
                            start = cdataStartSlashStar.end() - cdataStartSlashStar.start();
                            end = trimLen - (cdataEndSlashStar.end() - cdataEndSlashStar.start());
                            writer.write(trim.substring(start, end));
                        }
                        // case 4 start is /* */ end is //
                        else if ((null != cdataStartSlashStar.reset() && cdataStartSlashStar.find())
                                && (null != cdataEndSlashStar.reset() && cdataEndSlashSlash.find())) {
                            start = cdataStartSlashStar.end() - cdataStartSlashStar.start();
                            end = trimLen - (cdataEndSlashSlash.end() - cdataEndSlashSlash.start());
                            writer.write(trim.substring(start, end));
                        }
                        // case 5 no commented out cdata present.
                        else {
                            writer.write(result);
                        }
                    } else {
                        if (trim.startsWith("<![CDATA[") && trim.endsWith("]]>")) {
                            writer.write(trim.substring(9, trim.length() - 3));
                        } else {
                            writer.write(result);
                        }
                    }
                } else {
                    if (trim.startsWith("<!--") && trim.endsWith("//-->")) {
                        writer.write(trim.substring(4, trim.length() - 5));
                    } else {
                        writer.write(result);
                    }
                }
            }
            if (isXhtml) {
                if (!writingCdata) {
                    if (isScript) {
                        writer.write("\n//]]>\n");
                    } else {
                        writer.write("\n]]>\n");
                    }
                }
            } else {
                if (isScriptHidingEnabled) {
                    writer.write("\n//-->\n");
                }
            }
        }
        isScript = false;
        isStyle = false;
        if ("cdata".equalsIgnoreCase(name)) {
            writer.write("]]>");
            writingCdata = false;
            isCdata = false;
            return;
        }
        // See if we need to close the start of the last element
        if (closeStart) {
            boolean isEmptyElement = HtmlUtils.isEmptyElement(name);

            // Tricky: we need to use the writer ivar here, rather than the
            // one from the FacesContext because we don't want
            // spurious /> characters to appear in the output.
            if (isEmptyElement) {
                flushAttributes();
                writer.write(" />");
                closeStart = false;
                return;
            }
            flushAttributes();
            writer.write('>');
            closeStart = false;
        }

        writer.write("</");
        writer.write(name);
        writer.write('>');

    }

    /**
     * @return the character encoding, such as "ISO-8859-1" for this ResponseWriter. Refer to: <a
     *         href="http://www.iana.org/assignments/character-sets" >theIANA</a> for a list of character encodings.
     */
    @Override
    public String getCharacterEncoding() {

        return encoding;

    }

    /**
     * <p>
     * Write the text that should begin a response.
     * </p>
     *
     * @throws IOException if an input/output error occurs
     */
    @Override
    public void startDocument() throws IOException {

        // do nothing;

    }

    /**
     * <p>
     * Write the start of an element, up to and including the element name. Clients call <code>writeAttribute()</code>
     * or <code>writeURIAttribute()</code> methods to add attributes after calling this method.
     *
     * @param name Name of the starting element
     * @param componentForElement The UIComponent instance that applies to this element. This argument may be
     *            <code>null</code>.
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    @Override
    public void startElement(String name, UIComponent componentForElement) throws IOException {

        if (name == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "name"));
        }
        closeStartIfNecessary();
        isScriptOrStyle(name);
        scriptOrStyleSrc = false;
        if ("cdata".equalsIgnoreCase(name)) {
            isCdata = true;
            writingCdata = true;
            writer.write("<![CDATA[");
            closeStart = false;
            return;
        } else if (writingCdata) {
            // starting an element within a cdata section,
            // keep escaping disabled
            isCdata = false;
            writingCdata = true;
        }

        writer.write('<');
        writer.write(name);
        closeStart = true;

    }

    @Override
    public void write(char[] cbuf) throws IOException {

        closeStartIfNecessary();
        writer.write(cbuf);

    }

    @Override
    public void write(int c) throws IOException {

        closeStartIfNecessary();
        writer.write(c);

    }

    @Override
    public void write(String str) throws IOException {

        closeStartIfNecessary();
        writer.write(str);

    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {

        closeStartIfNecessary();
        writer.write(cbuf, off, len);

    }

    @Override
    public void write(String str, int off, int len) throws IOException {

        closeStartIfNecessary();
        writer.write(str, off, len);

    }

    /**
     * <p>
     * Write a properly escaped attribute name and the corresponding value. The value text will be converted to a String
     * if necessary. This method may only be called after a call to <code>startElement()</code>, and before the opened
     * element has been closed.
     * </p>
     *
     * @param name Attribute name to be added
     * @param value Attribute value to be added
     * @param componentPropertyName The name of the component property to which this attribute argument applies. This
     *            argument may be <code>null</code>.
     * @throws IllegalStateException if this method is called when there is no currently open element
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    @Override
    public void writeAttribute(String name, Object value, String componentPropertyName) throws IOException {

        if (name == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "name"));
        }
        if (value == null) {
            return;
        }

        if (isCdata) {
            return;
        }

        if (name.equalsIgnoreCase("src") && isScriptOrStyle()) {
            scriptOrStyleSrc = true;
        }

        Class<?> valueClass = value.getClass();

        // Output Boolean values specially
        if (valueClass == Boolean.class) {
            if (Boolean.TRUE.equals(value)) {
                // NOTE: HTML 4.01 states that boolean attributes
                // may legally take a single value which is the
                // name of the attribute itself or appear using
                // minimization.
                // http://www.w3.org/TR/html401/intro/sgmltut.html#h-3.3.4.2
                attributesBuffer.write(' ');
                attributesBuffer.write(name);
                attributesBuffer.write("=\"");
                attributesBuffer.write(name);
                attributesBuffer.write('"');
            }
        } else {
            attributesBuffer.write(' ');
            attributesBuffer.write(name);
            attributesBuffer.write("=\"");
            // write the attribute value
            String val = value.toString();
            ensureTextBufferCapacity(val);
            HtmlUtils.writeAttribute(attributesBuffer, escapeUnicode, escapeIso, buffer, val, textBuffer,
                    isScriptInAttributeValueEnabled);
            attributesBuffer.write('"');
        }

    }

    /**
     * <p>
     * Write a comment string containing the specified text. The text will be converted to a String if necessary. If
     * there is an open element that has been created by a call to <code>startElement()</code>, that element will be
     * closed first.
     * </p>
     *
     * @param comment Text content of the comment
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>comment</code> is <code>null</code>
     */
    @Override
    public void writeComment(Object comment) throws IOException {

        if (comment == null) {
            throw new NullPointerException(
                    MessageUtils.getExceptionMessageString(MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID));
        }

        if (writingCdata) {
            return;
        }

        closeStartIfNecessary();
        // Don't include a trailing space after the '<!--'
        // or a leading space before the '-->' to support
        // IE conditional commentsoth
        writer.write("<!--");
        writer.write(comment.toString());
        writer.write("-->");

    }

    /**
     * <p>
     * Write a properly escaped single character, If there is an open element that has been created by a call to
     * <code>startElement()</code>, that element will be closed first.
     * </p>
     * <p/>
     * <p>
     * All angle bracket occurrences in the argument must be escaped using the &amp;gt; &amp;lt; syntax.
     * </p>
     *
     * @param text Text to be written
     * @throws IOException if an input/output error occurs
     */
    public void writeText(char text) throws IOException {

        closeStartIfNecessary();
        writer.write(text);
    }

    /**
     * <p>
     * Write properly escaped text from a character array. The output from this command is identical to the invocation:
     * <code>writeText(c, 0, c.length)</code>. If there is an open element that has been created by a call to
     * <code>startElement()</code>, that element will be closed first.
     * </p>
     * </p>
     * <p/>
     * <p>
     * All angle bracket occurrences in the argument must be escaped using the &amp;gt; &amp;lt; syntax.
     * </p>
     *
     * @param text Text to be written
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>text</code> is <code>null</code>
     */
    public void writeText(char text[]) throws IOException {

        if (text == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "text"));
        }
        closeStartIfNecessary();
        writer.write(text);

    }

    /**
     * <p>
     * Write a properly escaped object. The object will be converted to a String if necessary. If there is an open
     * element that has been created by a call to <code>startElement()</code>, that element will be closed first.
     * </p>
     *
     * @param text Text to be written
     * @param componentPropertyName The name of the component property to which this text argument applies. This
     *            argument may be <code>null</code>.
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>text</code> is <code>null</code>
     */
    @Override
    public void writeText(Object text, String componentPropertyName) throws IOException {

        if (text == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "text"));
        }
        closeStartIfNecessary();
        writer.write(text.toString());
    }

    /**
     * <p>
     * Write properly escaped text from a character array. If there is an open element that has been created by a call
     * to <code>startElement()</code>, that element will be closed first.
     * </p>
     * <p/>
     * <p>
     * All angle bracket occurrences in the argument must be escaped using the &amp;gt; &amp;lt; syntax.
     * </p>
     *
     * @param text Text to be written
     * @param off Starting offset (zero-relative)
     * @param len Number of characters to be written
     * @throws IndexOutOfBoundsException if the calculated starting or ending position is outside the bounds of the
     *             character array
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>text</code> is <code>null</code>
     */
    @Override
    public void writeText(char text[], int off, int len) throws IOException {

        if (text == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "text"));
        }
        if (off < 0 || off > text.length || len < 0 || len > text.length) {
            throw new IndexOutOfBoundsException();
        }
        closeStartIfNecessary();
        writer.write(text, off, len);
    }

    /**
     * <p>
     * Write a properly encoded URI attribute name and the corresponding value. The value text will be converted to a
     * String if necessary). This method may only be called after a call to <code>startElement()</code>, and before the
     * opened element has been closed.
     * </p>
     *
     * @param name Attribute name to be added
     * @param value Attribute value to be added
     * @param componentPropertyName The name of the component property to which this attribute argument applies. This
     *            argument may be <code>null</code>.
     * @throws IllegalStateException if this method is called when there is no currently open element
     * @throws IOException if an input/output error occurs
     * @throws NullPointerException if <code>name</code> or <code>value</code> is <code>null</code>
     */
    @Override
    public void writeURIAttribute(String name, Object value, String componentPropertyName) throws IOException {

        if (name == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "name"));
        }
        if (value == null) {
            throw new NullPointerException(MessageUtils.getExceptionMessageString(
                    MessageUtils.NULL_PARAMETERS_ERROR_MESSAGE_ID, "value"));
        }

        if (isCdata) {
            return;
        }

        if (name.equalsIgnoreCase("src") && isScriptOrStyle()) {
            scriptOrStyleSrc = true;
        }

        attributesBuffer.write(' ');
        attributesBuffer.write(name);
        attributesBuffer.write("=\"");

        String stringValue = value.toString();
        ensureTextBufferCapacity(stringValue);
        // Javascript URLs should not be URL-encoded
        if (stringValue.startsWith("javascript:")) {
            HtmlUtils.writeAttribute(attributesBuffer, escapeUnicode, escapeIso, buffer, stringValue, textBuffer,
                    isScriptInAttributeValueEnabled);
        } else {
            HtmlUtils.writeURL(attributesBuffer, stringValue, textBuffer, encoding);
        }

        attributesBuffer.write('"');

    }

    // --------------------------------------------------------- Private
    // Methods

    private void ensureTextBufferCapacity(String source) {
        int len = source.length();
        if (textBuffer.length < len) {
            textBuffer = new char[len * 2];
        }
    }

    /**
     * This method automatically closes a previous element (if not already closed).
     *
     * @throws IOException if an error occurs writing
     */
    private void closeStartIfNecessary() throws IOException {

        if (closeStart) {
            flushAttributes();
            writer.write('>');
            closeStart = false;
            if (isScriptOrStyle() && !scriptOrStyleSrc) {
                isXhtml = getContentType().equals(RIConstants.XHTML_CONTENT_TYPE);
                if (isXhtml) {
                    if (!writingCdata) {
                        if (isScript) {
                            writer.write("\n//<![CDATA[\n");
                        } else {
                            writer.write("\n<![CDATA[\n");
                        }
                    }
                } else {
                    if (isScriptHidingEnabled) {
                        writer.write("\n<!--\n");
                    }
                }
                origWriter = writer;
                if (scriptBuffer == null) {
                    scriptBuffer = new FastStringWriter(1024);
                }
                scriptBuffer.reset();
                writer = scriptBuffer;
                isScript = false;
                isStyle = false;
            }
        }

    }

    private void flushAttributes() throws IOException {

        // a little complex, but the end result is, potentially, two
        // fewer temp objects created per call.
        StringBuilder b = attributesBuffer.getBuffer();
        int totalLength = b.length();
        if (totalLength != 0) {
            int curIdx = 0;
            while (curIdx < totalLength) {
                if ((totalLength - curIdx) > buffer.length) {
                    int end = curIdx + buffer.length;
                    b.getChars(curIdx, end, buffer, 0);
                    writer.write(buffer);
                    curIdx += buffer.length;
                } else {
                    int len = totalLength - curIdx;
                    b.getChars(curIdx, curIdx + len, buffer, 0);
                    writer.write(buffer, 0, len);
                    curIdx += len;
                }
            }
            attributesBuffer.reset();
        }

    }

    private boolean isScriptOrStyle(String name) {
        if ("script".equalsIgnoreCase(name)) {
            isScript = true;
        } else if ("style".equalsIgnoreCase(name)) {
            isStyle = true;
        } else {
            isScript = false;
            isStyle = false;
        }

        return (isScript || isStyle);
    }

    private boolean isScriptOrStyle() {
        return (isScript || isStyle);
    }
}
