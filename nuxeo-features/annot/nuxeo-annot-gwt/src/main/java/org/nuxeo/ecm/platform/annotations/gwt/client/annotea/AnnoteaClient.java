/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.annotea;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;

/**
 * @author Alexandre Russel
 */
public class AnnoteaClient {

    private static AnnoteaResponseManager responseManager;

    private static WebConfiguration webConfiguration;

    private RequestBuilder postRequest;

    private static AnnotationController controller;

    public AnnoteaClient(AnnotationController annotationController) {
        controller = annotationController;
        responseManager = new AnnoteaResponseManager(annotationController);
        webConfiguration = annotationController.getWebConfiguration();
    }

    public void submitAnnotation(Annotation newAnnotation) {
        AnnotationXmlGenerator xmlGenerator = new AnnotationXmlGenerator(webConfiguration, newAnnotation);
        String request = xmlGenerator.generateXml();
        postRequest = new RequestBuilder(RequestBuilder.POST, URL.encode(controller.getAnnoteaServerUrl()));
        try {
            postRequest.sendRequest(request, new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                    Window.alert("Error while sending data to annotea server: " + exception.toString());
                }

                public void onResponseReceived(Request request, Response response) {
                    responseManager.processSubmitAnnotationResponse(response.getText());
                    getAnnotationList(controller.getDocumentUrl());
                    controller.reloadAnnotations();
                }
            });
        } catch (RequestException e) {
            GWT.log("Error while sending new annotation", e);
            Log.debug("Error while sending new annotation", e);
        }
    }

    public void getAnnotationList(String annotates) {
        getAnnotationList(annotates, false);
    }

    public void getAnnotationList(String annotates, final boolean forceDecorate) {
        if (annotates.contains("?")) {
            annotates = annotates.substring(0, annotates.indexOf('?'));
        }
        String url = controller.getAnnoteaServerUrl() + "?w3c_annotates=" + annotates;
        RequestBuilder getRequest = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
        try {
            getRequest.sendRequest(null, new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                }

                public void onResponseReceived(Request request, Response response) {
                    responseManager.processAnnotationListResponse(response.getText());
                    if (forceDecorate) {
                        // Force all the annotations to be redecorated
                        controller.updateAnnotations(true);
                    }
                }
            });
        } catch (RequestException e) {
            GWT.log("Error while requesting annotations: " + url, e);
            Log.debug("Error while requesting annotations: " + url, e);
            Window.alert(e.toString());
        }
    }

    public void deleteAnnotation(String annotates, final Annotation annotation) {
        if (annotates.contains("?")) {
            annotates = annotates.substring(0, annotates.indexOf('?'));
        }

        String url = controller.getAnnoteaServerUrl() + "/" + annotation.getUUID();
        url += "?document_url=" + annotates;
        RequestBuilder req = new RequestBuilder("DELETE", url) {
            // nothing to override... used to make a *real* HTTP DELETE request
        };

        try {
            req.sendRequest(null, new RequestCallback() {
                public void onError(Request arg0, Throwable arg1) {
                }

                public void onResponseReceived(Request arg0, Response arg1) {
                    getAnnotationList(Window.Location.getHref(), true);
                    controller.reloadAnnotations();
                }
            });
        } catch (RequestException e) {
            GWT.log("Error while deleting an annotation: " + url, e);
            Log.debug("Error while deleting an annotation: " + url, e);
        }
    }

}
