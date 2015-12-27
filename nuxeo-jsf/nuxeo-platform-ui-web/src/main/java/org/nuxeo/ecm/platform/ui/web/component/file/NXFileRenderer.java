/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.fileupload.FileUploadBase.InvalidContentTypeException;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;

import com.sun.faces.renderkit.html_basic.FileRenderer;

/**
 * Renderer for file input.
 * <p>
 * Overrides the base JSF renderer for files to ignore error when submitting file inside a non multipart form.
 * <p>
 * Component {@link UIInputFile} will handle error message management in UI, if validation phase occurs.
 *
 * @since 7.1
 */
public class NXFileRenderer extends FileRenderer {

    public static final String RENDERER_TYPE = "javax.faces.NXFile";

    @Override
    public void decode(FacesContext context, UIComponent component) {

        rendererParamsNotNull(context, component);

        if (!shouldDecode(component)) {
            return;
        }

        String clientId = decodeBehaviors(context, component);

        if (clientId == null) {
            clientId = component.getClientId(context);
        }

        assert (clientId != null);
        ExternalContext externalContext = context.getExternalContext();
        Map<String, String> requestMap = externalContext.getRequestParameterMap();

        if (requestMap.containsKey(clientId)) {
            setSubmittedValue(component, requestMap.get(clientId));
        }

        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        try {
            Collection<Part> parts = request.getParts();
            for (Part cur : parts) {
                if (clientId.equals(cur.getName())) {
                    // Nuxeo patch: transform into serializable blob right away, and do not set component transient
                    // component.setTransient(true);
                    // setSubmittedValue(component, cur);
                    setSubmittedValue(component, FileUtils.createBlob(cur));
                }
            }
        } catch (IOException ioe) {
            throw new FacesException(ioe);
        } catch (ServletException se) {
            Throwable cause = se.getCause();
            // Nuxeo specific error management
            if ((cause instanceof InvalidContentTypeException)
                    || (cause != null && cause.getClass().getName().contains("InvalidContentTypeException"))) {
                setSubmittedValue(component, Blobs.createBlob(""));
            } else {
                throw new FacesException(se);
            }
        }
    }
}
