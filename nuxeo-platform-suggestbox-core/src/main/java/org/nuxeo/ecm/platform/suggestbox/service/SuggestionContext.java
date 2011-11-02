package org.nuxeo.ecm.platform.suggestbox.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

import com.sun.star.lang.IllegalArgumentException;

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

    public final String suggestionPoint;

    public final NuxeoPrincipal principal;

    public final Map<String, String> messages = new HashMap<String, String>();

    public transient CoreSession session;

    public DocumentModel currentDocument;

    public Locale locale = Locale.ENGLISH;

    public SuggestionContext(String suggestionPoint, NuxeoPrincipal principal)
            throws IllegalArgumentException {
        if (suggestionPoint == null) {
            throw new IllegalArgumentException(
                    "suggestionPoint is a mandatory field of the SuggestionContext");
        }
        if (principal == null) {
            throw new IllegalArgumentException(
                    "principal is a mandatory field of the SuggestionContext");
        }
        this.suggestionPoint = suggestionPoint;
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