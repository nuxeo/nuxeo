/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors: Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.user.center.profile;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.TimeZoneSelector;

/**
 * Provide from the system available timezones to be displayed in UI.
 *
 * @since 5.6
 */
@Scope(ScopeType.SESSION)
@Name("timeZones")
public class TimeZones implements Serializable {

    private static final long serialVersionUID = 1L;

    @In
    protected LocaleSelector localeSelector;

    private List<SelectItem> timeZoneSelectItems = null;

    public List<SelectItem> getTimeZones() {
        if (timeZoneSelectItems == null) {
            initTimeZones();
        }
        return timeZoneSelectItems;
    }

    public String displayCurrentTimeZone() {
        TimeZoneSelector tzs = TimeZoneSelector.instance();
        String timeZoneId = tzs.getTimeZoneId();
        if (StringUtils.isEmpty(timeZoneId)) {
            TimeZone timeZone = tzs.getTimeZone();
            if (timeZone != null) {
                timeZoneId = timeZone.getID();
            }
        }
        return displayTimeZone(timeZoneId);
    }

    public String displayTimeZone(String id) {
        if (id == null || id.trim().length() == 0 || "none".equals(id)) {
            return "";
        }
        return id + " - " + TimeZone.getTimeZone(id).getDisplayName(localeSelector.getLocale());
    }

    private void initTimeZones() {
        timeZoneSelectItems = new ArrayList<>();
        final String[] timeZoneIds = TimeZone.getAvailableIDs();
        for (final String id : timeZoneIds) {
            timeZoneSelectItems.add(new SelectItem(id, displayTimeZone(id)));
        }
        Collections.sort(timeZoneSelectItems, new Comparator<SelectItem>() {
            @Override
            public int compare(SelectItem o1, SelectItem o2) {
                return ((String) o1.getValue()).compareTo((String) o2.getValue());
            }
        });
    }
}
