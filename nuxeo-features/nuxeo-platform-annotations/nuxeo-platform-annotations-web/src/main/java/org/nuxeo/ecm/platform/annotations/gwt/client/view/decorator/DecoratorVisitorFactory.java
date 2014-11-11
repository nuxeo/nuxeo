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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.decorator;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationFrameApplication;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.StringRangeXPointer;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class DecoratorVisitorFactory {

    private DecoratorVisitorFactory() {
        // factory
    }

    public static DecoratorVisitor forAnnotation(Annotation annotation,
            AnnotationController controller) {
        if (annotation.hasStartContainer() && annotation.hasEndContainer()) {
            return new NuxeoDecoratorVisitor(annotation, controller);
        } else {
            StringRangeXPointer xp = (StringRangeXPointer) annotation.getXpointer();
            return new AnnoteaDecoratorVisitor(xp.getFirstNode(),
                    xp.getLength(), xp.getStartOffset(), annotation, controller);
        }
    }

    public static DecoratorVisitor forSelectedText(Annotation annotation) {
        return new NuxeoSelectedTextDecoratorVisitor(annotation,
                AnnotationFrameApplication.getController());
    }

}
