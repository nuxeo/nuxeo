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

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;

import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * @author <a href="mailto:bjalon@nuxeo.com">Benjamin JALON</a>
 */
public class AnnotationActionsBanner extends HorizontalPanel {

    public AnnotationActionsBanner(AnnotationController controller) {
        super();
        setStylePrimaryName("annotation-action-list");

        add(new HideShowAnnotationsButton(controller));
    }
}
