/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Base class and default implementation for passing contextual information to the suggestion service. This is primarily
 * a hash map to store arbitrary context element that might be useful for suggester along with a few mandatory and
 * common optional attributes for direct access.
 *
 * @author ogrisel
 */
public class SuggestionContext extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public final String suggesterGroup;

    public final Principal principal;

    public final Map<String, String> messages = new HashMap<>();

    public transient CoreSession session;

    public DocumentModel currentDocument;

    public Locale locale = Locale.ENGLISH;

    public SuggestionContext(String suggesterGroup, Principal principal) throws IllegalArgumentException {
        if (suggesterGroup == null) {
            throw new IllegalArgumentException("suggesterGroup is a mandatory field of the SuggestionContext");
        }
        if (principal == null) {
            throw new IllegalArgumentException("principal is a mandatory field of the SuggestionContext");
        }
        this.suggesterGroup = suggesterGroup;
        this.principal = principal;
    }

    public SuggestionContext withSession(CoreSession session) {
        this.session = session;
        return this;
    }

    public SuggestionContext withCurrentDocument(DocumentModel currentDocument) {
        this.currentDocument = currentDocument;
        return this;
    }

    public SuggestionContext withLocale(Locale locale) {
        this.locale = locale;
        return this;
    }

    public SuggestionContext withMessages(Map<String, String> messages) {
        this.messages.putAll(messages);
        return this;
    }

}
