/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * Contributors: Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.ecm.user.center.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import javax.faces.model.SelectItem;

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
public class TimeZones {

    @In LocaleSelector localeSelector;

    private List<SelectItem> timeZoneSelectItems = null;

    public List<SelectItem> getTimeZones() {
        if (timeZoneSelectItems == null) {
            initTimeZones();
        }
        return timeZoneSelectItems;
    }

    public String displayCurrentTimeZone() {
        TimeZoneSelector tzs = TimeZoneSelector.instance();
        return displayTimeZone(tzs.getTimeZoneId());
    }

    public String displayTimeZone(String id) {
        if (id == null || id.trim().length() == 0 || "none".equals(id)) {
            return "";
        }
        return id + " - " + TimeZone.getTimeZone(id).getDisplayName(localeSelector.getLocale());
    }

    private void initTimeZones() {
        timeZoneSelectItems = new ArrayList<SelectItem>();
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