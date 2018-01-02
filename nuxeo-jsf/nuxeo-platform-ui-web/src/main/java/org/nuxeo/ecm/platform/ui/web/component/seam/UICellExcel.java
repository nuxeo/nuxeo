/*
 * (C) Copyright 2008 JBoss and others.
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
 *     Original file from org.jboss.seam.excel.ui.UICell.java in jboss-seam-excel
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Interpolator;
import org.jboss.seam.excel.ExcelWorkbookException;
import org.jboss.seam.ui.util.JSF;
import org.nuxeo.ecm.platform.ui.web.util.NXHtmlResponseWriter;

import com.sun.faces.config.WebConfiguration;

/**
 * Override of Seam cell component to control HTML encoding of accents in excel, and to improve data type guessing when
 * using dates or numbers.
 *
 * @since 5.5
 */
public class UICellExcel extends org.jboss.seam.excel.ui.UICell {

    private static final Log log = LogFactory.getLog(UICellExcel.class);

    public static final String DEFAULT_CONTENT_TYPE = "text/html";

    public static final String DEFAULT_CHARACTER_ENCODING = "utf-8";

    // add field again as it's private in parent class
    protected Object value;

    /**
     * Force type attribute, added here to ensure value expression resolution
     */
    protected String forceType;

    /**
     * Style attribute, added here to ensure value expression resolution
     */
    protected String style;

    @Override
    public Object getValue() {
        Object theValue = valueOf("value", value);
        if (theValue == null) {
            try {
                theValue = cmp2String(FacesContext.getCurrentInstance(), this);
                String forceType = getForceType();
                if (forceType != null && !forceType.isEmpty()) {
                    theValue = convertStringToTargetType((String) theValue, forceType);
                }
            } catch (IOException e) {
                String message = Interpolator.instance().interpolate("Could not render cell #0", getId());
                throw new ExcelWorkbookException(message, e);
            }
        } else {
            theValue = theValue.toString();
        }
        return theValue;
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Converts string value as returned by widget to the target type for an accurate cell format in the XLS/CSV export.
     * <ul>
     * <li>If force type is set to "number", convert value to a double (null if empty).</li>
     * <li>If force type is set to "bool", convert value to a boolean (null if empty).</li>
     * <li>If force type is set to "date", convert value to a date using most frequent date parsers using the short,
     * medium, long and full formats and current locale, trying first with time information and after with only date
     * information. Returns null if date is empty or could not be parsed.</li>
     * </ul>
     *
     * @since 5.6
     */
    protected Object convertStringToTargetType(String value, String forceType) {
        if (CellType.number.name().equals(forceType)) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            return Double.valueOf(value);
        } else if (CellType.date.name().equals(forceType)) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            Locale locale = FacesContext.getCurrentInstance().getViewRoot().getLocale();
            int[] formats = { DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL };
            for (int format : formats) {
                try {
                    return DateFormat.getDateTimeInstance(format, format, locale).parse(value);
                } catch (ParseException e) {
                    // ignore
                }
                try {
                    return DateFormat.getDateInstance(format, locale).parse(value);
                } catch (ParseException e) {
                    // ignore
                }
            }
            log.warn("Could not convert value to a date instance: " + value);
            return null;
        } else if (CellType.bool.name().equals(forceType)) {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            return Boolean.valueOf(value);
        }
        return value;
    }

    /**
     * Helper method for rendering a component (usually on a facescontext with a caching reponsewriter)
     *
     * @param facesContext The faces context to render to
     * @param component The component to render
     * @return The textual representation of the component
     * @throws IOException If the JSF helper class can't render the component
     */
    public static String cmp2String(FacesContext facesContext, UIComponent component) throws IOException {
        ResponseWriter oldResponseWriter = facesContext.getResponseWriter();
        String contentType = oldResponseWriter != null ? oldResponseWriter.getContentType() : DEFAULT_CONTENT_TYPE;
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
        ResponseWriter newResponseWriter = new NXHtmlResponseWriter(cacheingWriter, contentType, characterEncoding,
                scriptHiding, scriptInAttributes, escaping);
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

    /**
     * Returns the style attribute, used to format cells with a specific {@link #forceType}. Sample value for dates
     * formatting: "xls-format-mask: #{nxu:basicDateFormatter()};".
     *
     * @since 5.6
     */
    @Override
    public String getStyle() {
        if (style != null) {
            return style;
        }
        ValueExpression ve = getValueExpression("style");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * @since 5.6
     */
    @Override
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Returns the force type attribute, used to force cell type to "date" or "number" for instance.
     *
     * @since 5.6
     */
    public String getForceType() {
        if (forceType != null) {
            return forceType;
        }
        ValueExpression ve = getValueExpression("forceType");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * @since 5.6
     */
    public void setForceType(String forceType) {
        this.forceType = forceType;
    }

    // state holder

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        forceType = (String) values[1];
        style = (String) values[2];
    }

    @Override
    public Object saveState(FacesContext context) {
        return new Object[] { super.saveState(context), forceType, style };
    }

}
