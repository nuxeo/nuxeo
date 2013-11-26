/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationFilter;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationChangeListener.ChangeEvent;

import com.google.gwt.core.client.GWT;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationModel implements AnnotationChangeNotifier {

    private static final Comparator<Annotation> ANNOTATION_DATE_COMPARATOR = new Comparator<Annotation>() {
        public int compare(Annotation o1, Annotation o2) {
            return o1.getDate().compareTo(o2.getDate());
        }
    };

    private Annotation newAnnotation;

    private List<Annotation> annotations = new ArrayList<Annotation>();

    private List<Annotation> filteredAnnotations;

    private AnnotationFilter filter;

    private List<AnnotationChangeListener> listeners = new ArrayList<AnnotationChangeListener>();

    public void addChangeListener(AnnotationChangeListener listener) {
        listeners.add(listener);
    }

    public Annotation getNewAnnotation() {
        return newAnnotation;
    }

    public void setNewAnnotation(Annotation newAnnotation) {
        this.newAnnotation = newAnnotation;
        notifyListener(ChangeEvent.annotation);
    }

    private void notifyListener(ChangeEvent ce) {
        GWT.log("Notifying listener.", null);
        for (AnnotationChangeListener listener : listeners) {
            listener.onChange(this, ce);
        }
    }

    public List<Annotation> getAnnotations() {
        if (filteredAnnotations != null) {
            return filteredAnnotations;
        } else {
            return annotations;
        }
    }

    public List<Annotation> getUnfilteredAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        Collections.sort(annotations, ANNOTATION_DATE_COMPARATOR);
        this.annotations = annotations;

        int id = 0;
        for (Annotation annotation : annotations) {
            annotation.setId(id++);
        }

        if (filter != null) {
            filteredAnnotations = filterAnnotations(filter);
        }
        notifyListener(ChangeEvent.annotationList);
    }

    public List<Annotation> filterAnnotations(AnnotationFilter filter) {
        List<Annotation> filteredAnnotations = new ArrayList<Annotation>();
        for (Annotation annotation : annotations) {
            if (filter.accept(annotation)) {
                filteredAnnotations.add(annotation);
            }
        }
        return filteredAnnotations;
    }

    public void setFilter(AnnotationFilter filter) {
        this.filter = filter;
        filteredAnnotations = filterAnnotations(filter);
        notifyListener(ChangeEvent.annotationList);
    }

    public AnnotationFilter getFilter() {
        return filter;
    }
}
