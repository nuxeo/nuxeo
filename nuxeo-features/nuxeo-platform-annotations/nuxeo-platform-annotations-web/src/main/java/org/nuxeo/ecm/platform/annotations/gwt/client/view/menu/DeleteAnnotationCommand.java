/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
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
