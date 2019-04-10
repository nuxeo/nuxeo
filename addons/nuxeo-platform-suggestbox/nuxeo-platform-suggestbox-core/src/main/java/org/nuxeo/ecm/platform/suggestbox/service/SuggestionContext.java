/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Base class and default implementation for passing contextual information to
 * the suggestion service.
 *
 * This is primarily a hash map to store arbitrary context element that might be
 * useful for suggester along with a few mandatory and common optional
 * attributes for direct access.
 *
 * @author ogrisel
 */
public class SuggestionContext extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public final String suggesterGroup;

    public final Principal principal;

    public final Map<String, String> messages = new HashMap<String, String>();

    public transient CoreSession session;

    public DocumentModel currentDocument;

    public Locale locale = Locale.ENGLISH;

    public SuggestionContext(String suggesterGroup, Principal principal)
            throws IllegalArgumentException {
        if (suggesterGroup == null) {
            throw new IllegalArgumentException(
                    "suggesterGroup is a mandatory field of the SuggestionContext");
        }
        if (principal == null) {
            throw new IllegalArgumentException(
                    "principal is a mandatory field of the SuggestionContext");
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