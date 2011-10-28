/*
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
 *     Original file from org.jboss.seam.excel.ui.UICell.java in jboss-seam-excel
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.io.StringWriter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.jboss.seam.core.Interpolator;
import org.jboss.seam.excel.ExcelWorkbookException;
import org.jboss.seam.ui.util.JSF;

import com.sun.faces.config.WebConfiguration;
import com.sun.faces.renderkit.html_basic.HtmlResponseWriter;

/**
 * Override of Seam cell component to control html encoding of accents in
 * excel.
 *
 * @since 5.5
 */
public class UICellExcel extends org.jboss.seam.excel.ui.UICell {

    public static final String DEFAULT_CONTENT_TYPE = "text/html";

    public static final String DEFAULT_CHARACTER_ENCODING = "utf-8";

    // add field again as it's private in parent class
    protected Object value;

    public Object getValue() {
        Object theValue = valueOf("value", value);
        if (theValue == null) {
            try {
                theValue = cmp2String(FacesContext.getCurrentInstance(), this);
            } catch (IOException e) {
                String message = Interpolator.instance().interpolate(
                        "Could not render cell #0", getId());
                throw new ExcelWorkbookException(message, e);
            }
        }
        return theValue;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Helper method for rendering a component (usually on a facescontext with
     * a caching reponsewriter)
     *
     * @param facesContext The faces context to render to
     * @param component The component to render
     * @return The textual representation of the component
     * @throws IOException If the JSF helper class can't render the component
     */
    public static String cmp2String(FacesContext facesContext,
            UIComponent component) throws IOException {
        ResponseWriter oldResponseWriter = facesContext.getResponseWriter();
        String contentType = oldResponseWriter != null ? oldResponseWriter.getContentType()
                : DEFAULT_CONTENT_TYPE;
        String characterEncoding = oldResponseWriter != null ? oldResponseWriter.getCharacterEncoding()
                : DEFAULT_CHARACTER_ENCODING;
        StringWriter cacheingWriter = new StringWriter();

        // XXX: create a response writer by hand, to control html escaping of
        // iso characters
        // take default values for these confs
        Boolean scriptHiding = Boolean.FALSE;
        Boolean scriptInAttributes = Boolean.TRUE;
        // force escaping to true
        WebConfiguration.DisableUnicodeEscaping escaping = WebConfiguration.DisableUnicodeEscaping.True;
        ResponseWriter newResponseWriter = new HtmlResponseWriter(
                cacheingWriter, contentType, characterEncoding, scriptHiding,
                scriptInAttributes, escaping);
        // ResponseWriter newResponseWriter = renderKit.createResponseWriter(
        // cacheingWriter, contentType, characterEncoding);

        facesContext.setResponseWriter(newResponseWriter);
        JSF.renderChild(facesContext, component);
        if (oldResponseWriter != null) {
            facesContext.setResponseWriter(oldResponseWriter);
        }
        cacheingWriter.flush();
        cacheingWriter.close();
        return cacheingWriter.toString();
    }

}
