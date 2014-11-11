/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.tag.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.component.html.HtmlCalendar;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.tag.MetaRuleset;
import com.sun.facelets.tag.Metadata;
import com.sun.facelets.tag.jsf.ComponentConfig;

/**
 * @since 5.4.2
 */
public class InputDateTimeTagHandler extends GenericHtmlComponentHandler {

    private static final Log log = LogFactory.getLog(InputDateTimeTagHandler.class);

    public InputDateTimeTagHandler(ComponentConfig config) {
        super(config);
    }

    @Override
    protected MetaRuleset createMetaRuleset(Class type) {
        MetaRuleset m = super.createMetaRuleset(type);

        // aliases for the old date time component compatibility
        m.alias("format", "datePattern");
        // showsTime is not configurable anymore
        m.ignore("showsTime");
        // locale ok
        // timeZone ok
        m.alias("styleClass", "inputStyle");
        m.alias("triggerLabel", "buttonLabel");
        m.alias("triggerImg", "buttonIcon");
        m.alias("triggerStyleClass", "buttonClass");

        // setup some default properties
        m.add(new TagMetaData());

        // onchange and onselect not working anymore, but keep them in case
        // this is solved one day
        // m.ignore("onchange");
        // m.ignore("onselect");

        return m;

    }

    class TagMetaData extends Metadata {

        public TagMetaData() {
            super();
        }

        public void applyMetadata(FaceletContext ctx, Object instance) {
            if (!(instance instanceof HtmlCalendar)) {
                log.error("Cannot apply date time component metadata, "
                        + "not a HtmlCalendar instance: " + instance);
                return;
            }
            HtmlCalendar c = (HtmlCalendar) instance;
            c.setPopup(true);
            c.setEnableManualInput(true);
            c.setShowApplyButton(false);
        }
    }

}
