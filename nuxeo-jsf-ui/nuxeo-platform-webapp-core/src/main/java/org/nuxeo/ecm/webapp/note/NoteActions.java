/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.note;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Scope(CONVERSATION)
@Name("noteActions")
public class NoteActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final Pattern PATTERN_TO_CHECK = Pattern.compile("(.*<img.*/files:files/.*/>.*)+", Pattern.DOTALL);

    protected static final String PATTERN_TO_REPLACE = "(<img.*?)%s(/files:files/.*?/>)";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected transient NavigationContext navigationContext;

    private static class LiveDocumentRefFinder extends UnrestrictedSessionRunner {

        private String liveDocumentRef;

        private final DocumentModel proxy;

        public LiveDocumentRefFinder(DocumentModel proxy) {
            super(proxy.getRepositoryName());
            this.proxy = proxy;
        }

        @Override
        public void run() {
            liveDocumentRef = proxy.getRef().toString();
            if (proxy.getSourceId() != null) {
                liveDocumentRef = proxy.getSourceId();
                DocumentModel version = session.getDocument(new IdRef(proxy.getSourceId()));
                if (version.getSourceId() != null) {
                    liveDocumentRef = version.getSourceId();
                }
            }
        }

        public String getLiveDocumentRef() {
            if (liveDocumentRef == null) {
                runUnrestricted();
            }
            return liveDocumentRef;
        }

    }

    /**
     * Translate the image links referencing attached files to use the docId of the current proxy or version. Do not
     * translate anything if we are on a live document.
     *
     * @param note the note content
     * @return the translated note content
     */
    public String translateImageLinks(String note) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!(currentDocument.isProxy() || currentDocument.isVersion())) {
            return note;
        }

        if (!hasImageLinksToTranslate(note)) {
            return note;
        }

        String docIdToReplace = null;
        if (currentDocument.isVersion()) {
            docIdToReplace = currentDocument.getSourceId();
        } else if (currentDocument.isProxy()) {
            docIdToReplace = new LiveDocumentRefFinder(currentDocument).getLiveDocumentRef();
        }

        return translateImageLinks(note, docIdToReplace, currentDocument.getId());
    }

    protected boolean hasImageLinksToTranslate(String note) {
        Matcher matcher = PATTERN_TO_CHECK.matcher(note);
        return matcher.matches();
    }

    protected String translateImageLinks(String note, String fromDocRef, String toDocRef) {
        String patternToReplace = String.format(PATTERN_TO_REPLACE, fromDocRef);
        Pattern pattern = Pattern.compile(patternToReplace);
        Matcher matcher = pattern.matcher(note);
        String replacement = "$1" + toDocRef + "$2";
        return matcher.replaceAll(replacement);
    }

}
