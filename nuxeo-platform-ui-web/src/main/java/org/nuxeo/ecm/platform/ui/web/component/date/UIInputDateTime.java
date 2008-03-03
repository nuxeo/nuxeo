/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: UIInputDateTime.java 25667 2007-10-04 12:30:33Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.component.date;

import java.util.TimeZone;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.DateTimeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Component that displays a javascript calendar for date/time selection.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class UIInputDateTime extends UIInput {

    public static final String COMPONENT_TYPE = UIInputDateTime.class.getName();

    public static final String COMPONENT_FAMILY = UIInput.COMPONENT_FAMILY;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(UIInputDateTime.class);

    // attributes

    private String format;

    private Boolean showsTime;

    private String locale;

    private String triggerLabel;

    public UIInputDateTime() {
        setRendererType(COMPONENT_TYPE);
    }

    /**
     * Overriden to set converter as date time by default.
     */
    @Override
    public Converter getConverter() {
        return getDateTimeConverter();
    }

    public DateTimeConverter getDateTimeConverter() {
        DateTimeConverter converter = new DateTimeConverter();
        converter.setTimeZone(TimeZone.getDefault());
        converter.setPattern(getFormat());
        return converter;
    }

    // setters & getters
    public String getFormat() {
        if (format != null) {
            return format;
        }
        ValueExpression ve = getValueExpression("format");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return "dd/MM/yyyy HH:mm";
        }
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Boolean getShowsTime() {
        if (showsTime != null) {
            return showsTime;
        }
        ValueExpression ve = getValueExpression("showsTime");
        if (ve != null) {
            try {
                return (!Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext())));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    public void setShowsTime(Boolean showsTime) {
        this.showsTime = showsTime;
    }

    public String getLocale() {
        if (locale != null) {
            return locale;
        }
        ValueExpression ve = getValueExpression("locale");
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

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTriggerLabel() {
        if (triggerLabel != null) {
            return triggerLabel;
        }
        ValueExpression ve = getValueExpression("triggerLabel");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return "...";
        }
    }

    public void setTriggerLabel(String triggerLabel) {
        this.triggerLabel = triggerLabel;
    }

    // state holder

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[]{ super.saveState(context), format,
                showsTime, locale, triggerLabel, };
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        format = (String) values[1];
        showsTime = (Boolean) values[2];
        locale = (String) values[3];
        triggerLabel = (String) values[4];
    }
}
