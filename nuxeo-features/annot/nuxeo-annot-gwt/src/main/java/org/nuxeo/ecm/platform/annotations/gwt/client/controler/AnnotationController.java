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
package org.nuxeo.ecm.platform.annotations.gwt.client.controler;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.annotea.AnnoteaClient;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationChangeListener;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationModel;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Point;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.StringRangeXPointer;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointerFactory;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.AnnotatedDocument;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.NewAnnotationPopup;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.listener.AnnotationPopupEventListener;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Window;

/**
 * @author Alexandre Russel
 */
public class AnnotationController {

    private static AnnotationController CURRENT_INSTANCE;

    private static AnnotationModel model = new AnnotationModel();

    private static NewAnnotationPopup newAnnotationPopup;

    private static AnnotatedDocument annotatedDocument;

    private static final List<AnnotationPopupEventListener> annotationPopupListeners = new ArrayList<AnnotationPopupEventListener>();

    private String xPointerFilter;

    private String pointerAdapter;

    private boolean annotateImageOnly;

    private boolean multiImage;

    private final AnnoteaClient annoteaClient;

    private final WebConfiguration webConfiguration;

    private int frameScrollFromTop;

    private boolean creationPopupOpened = false;

    private final boolean onFrame;

    public AnnotationController(WebConfiguration webConfiguration, boolean onFrame) {
        this.onFrame = onFrame;
        this.webConfiguration = (webConfiguration == null ? WebConfiguration.DEFAULT_WEB_CONFIGURATION
                : webConfiguration);
        annoteaClient = new AnnoteaClient(this);

        CURRENT_INSTANCE = this;

        if (onFrame) {
            annotatedDocument = new AnnotatedDocument(this);
            model.addChangeListener(annotatedDocument);
            registerOnFrameMethods();
        } else {
            registerMainModuleMethods();
        }

    }

    private native void registerOnFrameMethods() /*-{
                                                 top['addNewAnnotation'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::addNewAnnotation();
                                                 top['showAnnotations'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::showAnnotations();
                                                 top['hideAnnotations'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::hideAnnotations();
                                                 top['updateSelectedAnnotation'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::updateSelectedAnnotation(I);
                                                 top['cancelNewAnnotationPopup'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::cancelNewAnnotationPopup();
                                                 top['loadAnnotationsOnFrame'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::loadAnnotations();
                                                 top['isAnnotationsVisible'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::isAnnotationsVisible();
                                                 top['deleteAnnotation'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::deleteAnnotationOnFrame(I);
                                                 }-*/;

    private native void registerMainModuleMethods() /*-{
                                                    top['loadAnnotationsOnMainModule'] = this.@org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::loadAnnotations();
                                                    }-*/;

    public void addNewAnnotation() {
        NewAnnotationPopup popup = getNewAnnotationPopup();
        Annotation annotation = getNewAnnotation();
        if (popup != null && annotation != null) {
            if (annotation.getXpointer() instanceof StringRangeXPointer) {
                // pre-decorate the selected text
                annotatedDocument.decorateSelectedText(annotation);
            }
            popup.show();
        }
    }

    public WebConfiguration getWebConfiguration() {
        return webConfiguration;
    }

    public AnnotatedDocument getAnnotatedDocument() {
        return annotatedDocument;
    }

    public void setXPointerFilter(String pointerFilter) {
        xPointerFilter = pointerFilter;
    }

    public void setPointerAdapter(String pointerAdapter) {
        this.pointerAdapter = pointerAdapter;
    }

    public boolean canAnnotate() {
        return webConfiguration.canAnnotate();
    }

    public void addModelChangeListener(AnnotationChangeListener listener) {
        model.addChangeListener(listener);
    }

    public int getFrameScrollFromTop() {
        return frameScrollFromTop;
    }

    public void setFrameScrollFromTop(int frameScrollFromTop) {
        this.frameScrollFromTop = frameScrollFromTop;
    }

    public boolean canCreateNewCreationPopup() {
        return !creationPopupOpened;
    }

    public void openCreationPopup() {
        creationPopupOpened = true;
    }

    public void closeCreationPopup() {
        creationPopupOpened = false;
    }

    public void newAnnotationCreated(Annotation annotation) {
        model.setNewAnnotation(annotation);
    }

    public void submitNewAnnotation() {
        GWT.log("submiting new annotation", null);
        Annotation newAnnotation = model.getNewAnnotation();
        annoteaClient.submitAnnotation(newAnnotation);
        model.setNewAnnotation(null);

        // set the selected annotation to the newly created one
        setSelectedAnnotationIndex(model.getAnnotations().size());
    }

    private native void setSelectedAnnotationIndex(int index) /*-{
                                                              top['selectedAnnotationIndex'] = index;
                                                              }-*/;

    public void reloadAnnotations() {
        if (onFrame) {
            reloadAnnotationsOnMainModule();
        } else {
            reloadAnnotationsOnFrame();
        }
    }

    private native void reloadAnnotationsOnMainModule() /*-{
                                                        top['loadAnnotationsOnMainModule']();
                                                        }-*/;

    private native void reloadAnnotationsOnFrame() /*-{
                                                   top['loadAnnotationsOnFrame']();
                                                   }-*/;

    public void cancelNewAnnotation() {
        model.setNewAnnotation(null);
    }

    public void cancelNewAnnotationPopup() {
        if (newAnnotationPopup != null) {
            newAnnotationPopup.cancel();
        }
    }

    public void createNewAnnotation(String pointer) {
        String href = getDocumentUrl();
        // Hardcoded url codec....
        String xpointerURI = href.substring(0, href.lastIndexOf("@") + 1) + pointer;
        Annotation newAnnotation = new Annotation(XPointerFactory.getXPointer(xpointerURI));
        model.setNewAnnotation(newAnnotation);
    }

    public void setAnnotationList(List<Annotation> annotations) {
        model.setAnnotations(annotations);
    }

    public native String getAnnoteaServerUrl() /*-{
                                               return top['annoteaServerUrl'];
                                               }-*/;

    public Annotation getNewAnnotation() {
        return model.getNewAnnotation();
    }

    public void loadAnnotations() {
        if (onFrame) {
            if (!isMultiImage()) {
                annotatedDocument.preDecorateDocument();
            }
        }
        annoteaClient.getAnnotationList(getDocumentUrl());
    }

    public native String getDocumentUrl() /*-{
                                          return top['docUrl'];
                                          }-*/;

    public void decorateDocument() {
        Log.debug("decorate document");
        annotatedDocument.preDecorateDocument();
        updateAnnotation(true);
    }

    public void updateSelectedAnnotation(int index) {
        annotatedDocument.updateSelectedAnnotation(index);
    }

    public void setFirstAnnotationSelected() {
        if (!model.getAnnotations().isEmpty()) {
            annotatedDocument.updateSelectedAnnotation(0);
        }
    }

    public void setCancelNewAnnotation() {
        model.setNewAnnotation(null);
    }

    public void setImageOnly(boolean b) {
        this.annotateImageOnly = b;
    }

    public boolean isImageOnly() {
        return this.annotateImageOnly;
    }

    public void setMultiImage(boolean b) {
        this.multiImage = b;
    }

    public boolean isMultiImage() {
        return this.multiImage;
    }

    public String filterXPointer(ImageElement image, String xpath, int i, int j, int k, int l) {
        if (xPointerFilter != null) {
            return filter(xPointerFilter, image, xpath, i, j, k, l);
        }
        return "#xpointer(image-range(" + xpath + ",[" + i + "," + j + "],[" + k + "," + l + "]))";
    }

    public native String filter(String xPointerFilter, ImageElement image, String xpath, int i, int j, int k, int l) /*-{
                                                                                                                     if(xPointerFilter && top[xPointerFilter]) {
                                                                                                                     return top[xPointerFilter](image, xpath, i, j, k, l);
                                                                                                                     }
                                                                                                                     }-*/;

    public Point[] filterAnnotation(Point topLeft, Point bottomRight) {
        if (pointerAdapter == null) {
            return new Point[] { topLeft, bottomRight };
        }
        String result = filterPoint(pointerAdapter, topLeft.getX(), topLeft.getY(), bottomRight.getX(),
                bottomRight.getY());
        if (result.equals("")) {
            return null;
        }
        String[] points = result.split(":");
        return new Point[] { new Point(points[0]), new Point(points[1]) };
    }

    private native String filterPoint(String pointerAdapter, int x, int y, int x2, int y2) /*-{
                                                                                           if(pointerAdapter && top[pointerAdapter]) {
                                                                                           return top[pointerAdapter](x, y, x2, y2);
                                                                                           }
                                                                                           }-*/;

    public static void updateAnnotation(boolean forceDecorate) {
        CURRENT_INSTANCE.updateAnnotations(forceDecorate);
    }

    public static void updateAnnotation() {
        updateAnnotation(false);
    }

    public void updateAnnotations(boolean forceDecorate) {
        annotatedDocument.update(forceDecorate);
    }

    public native void setAnnotationDecoratorFunction(String annotationDecoratorFunction) /*-{
                                                                                          top[annotationDecoratorFunction] = @org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController::updateAnnotation(Z);
                                                                                          }-*/;

    public void setNewAnnotationPopup(NewAnnotationPopup popup) {
        newAnnotationPopup = popup;
    }

    public NewAnnotationPopup getNewAnnotationPopup() {
        return newAnnotationPopup;
    }

    public void hideAnnotations() {
        annotatedDocument.hideAnnotations();
        disablePopupListeners();
    }

    public void disablePopupListeners() {
        for (AnnotationPopupEventListener listener : annotationPopupListeners) {
            listener.disable();
        }
    }

    public void showAnnotations() {
        annotatedDocument.showAnnotations();
        enablePopupListeners();
    }

    public void enablePopupListeners() {
        for (AnnotationPopupEventListener listener : annotationPopupListeners) {
            listener.enable();
        }
    }

    public void registerAnnotationPopupListener(AnnotationPopupEventListener listener) {
        annotationPopupListeners.add(listener);
    }

    public void removeAnnotationPopupListener(AnnotationPopupEventListener listener) {
        annotationPopupListeners.remove(listener);
    }

    public String getDecorateClassName() {
        if (annotatedDocument.isAnnotationsVisible()) {
            return AnnotationConstant.DECORATE_CLASS_NAME;
        } else {
            return AnnotationConstant.DECORATE_NOT_CLASS_NAME;
        }
    }

    public native void deleteAnnotation(int index) /*-{
                                                   top['deleteAnnotation'](index);
                                                   }-*/;

    @SuppressWarnings("unused")
    private void deleteAnnotationOnFrame(int index) {
        annoteaClient.deleteAnnotation(Window.Location.getHref(), model.getAnnotations().get(index));
    }

    public native boolean isAnnotationsVisible() /*-{
                                                 if (top && typeof top['annotationsShown'] != "undefined") {
                                                 return top['annotationsShown'];
                                                 } else {
                                                 return true;
                                                 }
                                                 }-*/;

    public void removeSelectedTextDecoration() {
        annotatedDocument.removeSelectedTextDecoration(getNewAnnotation());
    }

}
