/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
                    String filename = FileUtils.retrieveFilename(cur);
                    String mimetype = cur.getContentType();
                    setSubmittedValue(component,
                            FileUtils.createSerializableBlob(cur.getInputStream(), filename, mimetype));
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
