/*
 * (C) Copyright 2010-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Suggest to navigate to a specific group profile.
 */
public class GroupSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    private static final String PREFIX = "group";

    protected final String groupId;

    public GroupSuggestion(String groupId, String label, String iconURL) {
        super(groupId, CommonSuggestionTypes.GROUP, label, iconURL);
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getObjectUrl() {
        List<String> items = new ArrayList<String>();
        items.add(PREFIX);
        items.add(groupId);
        return StringUtils.join(items, "/");
    }
}
