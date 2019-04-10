/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
package org.nuxeo.ecm.platform.video;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Interpolator;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.UserAgentMatcher;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.video.service.VideoService;

/**
 * @author "<a href=\"mailto:bjalon@nuxeo.com\">Benjamin JALON</a>"
 */
@Name("videoActions")
@Install(precedence = Install.FRAMEWORK)
public class VideoActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true)
    protected VideoService videoService;

    public String getURLForPlayer(DocumentModel doc) {
        return DocumentModelFunctions.bigFileUrl(doc, "file:content", "");
    }

    public String getTranscodedVideoURL(DocumentModel doc, String name) {
        TranscodedVideo transcodedVideo = getTranscodedVideo(doc, name);
        if (transcodedVideo == null) {
            return null;
        }

        String blobPropertyName = transcodedVideo.getBlobPropertyName();
        return DocumentModelFunctions.bigFileUrl(doc, blobPropertyName, transcodedVideo.getBlob().getFilename());
    }

    public TranscodedVideo getTranscodedVideo(DocumentModel doc, String name) {
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        return videoDocument.getTranscodedVideo(name);
    }

    public String getURLForStaticPreview(DocumentModel videoDoc) {
        String lastModification = "" + (((Calendar) videoDoc.getPropertyValue("dc:modified")).getTimeInMillis());
        return DocumentModelFunctions.fileUrl("downloadPicture", videoDoc, "StaticPlayerView:content", lastModification);
    }

    public VideoConversionStatus getVideoConversionStatus(DocumentModel doc, String conversionName) {
        return videoService.getProgressStatus(doc.getRepositoryName(), doc.getId(), conversionName);
    }

    public String getStatusMessageFor(VideoConversionStatus status) {
        if (status == null) {
            return "";
        }
        String i18nMessageTemplate = messages.get(status.getMessage());
        if (i18nMessageTemplate == null) {
            return "";
        } else {
            return Interpolator.instance().interpolate(i18nMessageTemplate, status.positionInQueue, status.queueSize);
        }
    }

    public void launchConversion(DocumentModel doc, String conversionName) {
        videoService.launchConversion(doc, conversionName);
    }

    public boolean isSafariHTML5() {
        return UserAgentMatcher.isSafari5(getUserAgent());
    }

    public boolean isChromeHTML5() {
        return UserAgentMatcher.isChrome(getUserAgent());
    }

    public boolean isFirefoxHTML5() {
        return UserAgentMatcher.isFirefox4OrMore(getUserAgent());
    }

    protected String getUserAgent() {
        ExternalContext econtext = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) econtext.getRequest();
        return request.getHeader("User-Agent");
    }
}
