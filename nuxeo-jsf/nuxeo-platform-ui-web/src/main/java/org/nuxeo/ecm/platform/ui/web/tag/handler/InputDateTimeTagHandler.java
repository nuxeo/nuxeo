/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.faces.view.facelets.ComponentConfig;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.MetaRule;
import javax.faces.view.facelets.MetaRuleset;
import javax.faces.view.facelets.Metadata;
import javax.faces.view.facelets.MetadataTarget;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagAttributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.TimeZoneSelector;
import org.richfaces.component.UICalendar;

/**
 * @since 5.4.2
 */
public class InputDateTimeTagHandler extends GenericHtmlComponentHandler {

    private static final Log log = LogFactory.getLog(InputDateTimeTagHandler.class);

    protected final String defaultTime;

    /**
     * @since 5.7.2
     */
    protected TagAttributes attributes;

    public InputDateTimeTagHandler(ComponentConfig config) {
        super(config);
        attributes = config.getTag().getAttributes();
        defaultTime = getValue(attributes, "defaultTime", "12:00");
    }

    protected String getValue(TagAttributes attrs, String name, String defaultValue) {
        TagAttribute attr = attrs.get(name);
        if (attr == null) {
            return defaultValue;
        }
        return attr.getValue();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected MetaRuleset createMetaRuleset(Class type) {
        MetaRuleset m = super.createMetaRuleset(type);

        // aliases for the old date time component compatibility
        m.alias("format", "datePattern");
        // showsTime is not configurable anymore
        m.ignore("showsTime");
        // locale ok
        // timeZone ok, just need to convert string to a TimeZone instance
        m.addRule(new TimeZoneMetaRule());
        // do not bind styleClass to inputClass anymore: styleClass can be
        // taken into account but the datetime widget itself, see NXP-14963.
        // m.alias("styleClass", "inputClass");
        m.alias("triggerLabel", "buttonLabel");
        m.alias("triggerImg", "buttonIcon");
        m.alias("triggerStyleClass", "buttonClass");

        m.alias("onclick", "oninputclick");

        // setup some default properties
        m.add(new TagMetaData());

        // onchange and onselect not working anymore, but keep them in case
        // this is solved one day
        // m.ignore("onchange");
        // m.ignore("onselect");

        return m;

    }

    class TimeZoneMetaRule extends MetaRule {

        @Override
        public Metadata applyRule(String name, TagAttribute attribute, MetadataTarget meta) {
            if (!"timeZone".equals(name)) {
                return null;
            }
            return new Metadata() {
                @Override
                public void applyMetadata(FaceletContext ctx, Object instance) {
                    Object tz = attribute.getObject(ctx);
                    if (tz instanceof TimeZone) {
                        ((UICalendar) instance).setTimeZone((TimeZone) tz);
                    } else if (tz instanceof String) {
                        ((UICalendar) instance).setTimeZone(TimeZone.getTimeZone((String) tz));
                    } else {
                        throw new IllegalArgumentException("Invalid timezone: " + tz);
                    }
                }
            };
        }
    }

    class TagMetaData extends Metadata {

        public TagMetaData() {
            super();
        }

        @Override
        public void applyMetadata(FaceletContext ctx, Object instance) {
            if (!(instance instanceof UICalendar)) {
                log.error("Cannot apply date time component metadata, " + "not a HtmlCalendar instance: " + instance);
                return;
            }
            UICalendar c = (UICalendar) instance;
            TimeZone tz = c.getTimeZone();
            if (tz == null) {
                // set default timezone only if not already specified in the component
                c.setTimeZone(TimeZoneSelector.instance().getTimeZone());
            }
            c.setLocale(LocaleSelector.instance().getLocale());
        }
    }

    @Override
    public void setAttributes(FaceletContext ctx, Object instance) {
        super.setAttributes(ctx, instance);
        // set default time in timezone
        UICalendar c = (UICalendar) instance;
        c.setPopup(Boolean.parseBoolean(getValue(attributes, "popup", "true")));
        c.setEnableManualInput(Boolean.parseBoolean(getValue(attributes, "enableManualInput", "true")));
        c.setShowApplyButton(Boolean.parseBoolean(getValue(attributes, "showApplyButton", "false")));
        c.setZindex(Integer.parseInt(getValue(attributes, "zindex", "1500")));
        setDefaultTime(c);
    }

    protected void setDefaultTime(UICalendar instance) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        format.setTimeZone(instance.getTimeZone());
        Date date;
        try {
            date = format.parse(defaultTime);
        } catch (ParseException e) {
            return;
        }
        instance.setDefaultTime(date);
    }
}
