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

import org.apache.commons.lang.StringUtils;

/**
 * Suggest to navigate to a specific user profile.
 */
public class UserSuggestion extends Suggestion {

    private static final long serialVersionUID = 1L;

    private static final String PREFIX = "user";

    protected final String userId;

    public UserSuggestion(String userId, String label, String iconURL) {
        super(userId, CommonSuggestionTypes.USER, label, iconURL);
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String getObjectUrl() {
        List<String> items = new ArrayList<>();
        items.add(PREFIX);
        items.add(userId);
        return StringUtils.join(items, "/");
    }
}
