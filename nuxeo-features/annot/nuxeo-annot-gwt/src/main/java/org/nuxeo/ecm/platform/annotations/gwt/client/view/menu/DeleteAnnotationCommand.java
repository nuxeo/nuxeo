/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     troger
 */
package org.nuxeo.ecm.platform.annotations.gwt.client.view.menu;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.AbstractAnnotationCommand;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class DeleteAnnotationCommand extends AbstractAnnotationCommand {

    public DeleteAnnotationCommand(String title, AnnotationController controller, int annotationIndex) {
        super(title, controller, annotationIndex);
    }

    @Override
    protected void onExecute() {
        TranslationConstants translationConstants = GWT.create(TranslationConstants.class);
        if (Window.confirm(translationConstants.menuConfirmDelete())) {
            controller.deleteAnnotation(annotationIndex);
        }
    }

}
