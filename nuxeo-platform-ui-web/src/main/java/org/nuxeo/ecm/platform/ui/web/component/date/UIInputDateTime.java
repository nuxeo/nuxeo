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
 */
public class UIInputDateTime extends UIInput {

    public static final String COMPONENT_TYPE = UIInputDateTime.class.getName();

    public static final String COMPONENT_FAMILY = UIInput.COMPONENT_FAMILY;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(UIInputDateTime.class);

    // attributes

    protected String format;

    protected Boolean showsTime;

    protected String locale;

    protected String timeZone;

    protected String triggerLabel;

    protected String triggerImg;

    protected String onchange;

    protected String onclick;

    protected String onselect;

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
        String timeZone = getTimeZone();
        converter.setTimeZone(TimeZone.getTimeZone(timeZone));
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

    public String getTimeZone() {
        if (timeZone != null) {
            return timeZone;
        }
        ValueExpression ve = getValueExpression("timeZone");
        if (ve != null) {
            try {
                Object t = ve.getValue(getFacesContext().getELContext());
                if (t instanceof TimeZone) {
                    timeZone = ((TimeZone) t).getID();
                } else if (t instanceof String) {
                    timeZone = (String) t;
                }
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            timeZone = TimeZone.getDefault().getID();
        }
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getShowsTime() {
        if (showsTime != null) {
            return showsTime;
        }
        ValueExpression ve = getValueExpression("showsTime");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return true;
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

    public String getTriggerImg() {
        if (triggerImg != null) {
            return triggerImg;
        }
        ValueExpression ve = getValueExpression("triggerImg");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return getDefaultTriggerImg();
        }
    }

    public String getDefaultTriggerImg() {
        return "/jscalendar/calendar.png";
    }

    public void setTriggerImg(String triggerImg) {
        this.triggerImg = triggerImg;
    }

    protected String getStringValue(String name, String defaultValue) {
        ValueExpression ve = getValueExpression(name);
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return defaultValue;
        }
    }

    public String getOnchange() {
        if (onchange != null) {
            return onchange;
        }
        return getStringValue("onchange", null);
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

    public String getOnclick() {
        if (onclick != null) {
            return onclick;
        }
        return getStringValue("onclick", null);
    }

    public void setOnclick(String onclick) {
        this.onclick = onclick;
    }

    public String getOnselect() {
        if (onselect != null) {
            return onselect;
        }
        return getStringValue("onselect", null);
    }

    public void setOnselect(String onselect) {
        this.onselect = onselect;
    }

    // state holder

    @Override
    public Object saveState(FacesContext context) {
        return new Object[] { super.saveState(context), format, showsTime,
                locale, timeZone, triggerLabel, onchange, onclick, onselect };
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        format = (String) values[1];
        showsTime = (Boolean) values[2];
        locale = (String) values[3];
        timeZone = (String) values[4];
        triggerLabel = (String) values[5];
        onchange = (String) values[6];
        onclick = (String) values[7];
        onselect = (String) values[8];
    }

}
