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
 */
public class DecoratorVisitorFactory {

    private DecoratorVisitorFactory() {
        // factory
    }

    public static DecoratorVisitor forAnnotation(Annotation annotation, AnnotationController controller) {
        if (annotation.hasStartContainer() && annotation.hasEndContainer()) {
            return new NuxeoDecoratorVisitor(annotation, controller);
        } else {
            StringRangeXPointer xp = (StringRangeXPointer) annotation.getXpointer();
            return new AnnoteaDecoratorVisitor(xp.getFirstNode(), xp.getLength(), xp.getStartOffset(), annotation,
                    controller);
        }
    }

    public static DecoratorVisitor forSelectedText(Annotation annotation) {
        return new NuxeoSelectedTextDecoratorVisitor(annotation, AnnotationFrameApplication.getController());
    }

}
