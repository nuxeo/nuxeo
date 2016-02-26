/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.validator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.application.FacesMessage;
import javax.faces.component.PartialStateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.validation.ConstraintViolation;
import org.nuxeo.ecm.core.api.validation.DocumentValidationReport;
import org.nuxeo.ecm.core.api.validation.DocumentValidationService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.platform.el.DocumentPropertyContext;
import org.nuxeo.ecm.platform.ui.web.model.ProtectedEditableModel;
import org.nuxeo.ecm.platform.ui.web.validator.ValueExpressionAnalyzer.ListItemMapper;
import org.nuxeo.runtime.api.Framework;

/**
 * JSF validator for {@link DocumentModel} field constraints.
 *
 * @since 7.2
 */
public class DocumentConstraintValidator implements Validator, PartialStateHolder {

    private static final Log log = LogFactory.getLog(DocumentConstraintValidator.class);

    public static final String VALIDATOR_ID = "DocumentConstraintValidator";

    private boolean transientValue = false;

    private boolean initialState;

    protected Boolean handleSubProperties;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (context == null) {
            throw new NullPointerException();
        }
        if (component == null) {
            throw new NullPointerException();
        }
        ValueExpression ve = component.getValueExpression("value");
        if (ve == null) {
            return;
        }

        ValueExpressionAnalyzer expressionAnalyzer = new ValueExpressionAnalyzer(ve);
        ValueReference vref = expressionAnalyzer.getReference(context.getELContext());

        if (log.isDebugEnabled()) {
            log.debug(String.format("Validating  value '%s' for expression '%s', base=%s, prop=%s", value,
                    ve.getExpressionString(), vref.getBase(), vref.getProperty()));
        }

        if (isResolvable(vref, ve)) {
            List<ConstraintViolation> violations = doValidate(context, vref, ve, value);
            if (violations != null && !violations.isEmpty()) {
                Locale locale = context.getViewRoot().getLocale();
                if (violations.size() == 1) {
                    ConstraintViolation v = violations.iterator().next();
                    String msg = v.getMessage(locale);
                    throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
                } else {
                    Set<FacesMessage> messages = new LinkedHashSet<FacesMessage>(violations.size());
                    for (ConstraintViolation v : violations) {
                        String msg = v.getMessage(locale);
                        messages.add(new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
                    }
                    throw new ValidatorException(messages);
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean isResolvable(ValueReference ref, ValueExpression ve) {
        if (ve == null || ref == null) {
            return false;
        }
        Object base = ref.getBase();
        if (base != null) {
            Class baseClass = base.getClass();
            if (baseClass != null) {
                if (DocumentPropertyContext.class.isAssignableFrom(baseClass)
                        || (Property.class.isAssignableFrom(baseClass))
                        || (ProtectedEditableModel.class.isAssignableFrom(baseClass))
                        || (ListItemMapper.class.isAssignableFrom(baseClass))) {
                    return true;
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("NOT validating %s, base=%s, prop=%s", ve.getExpressionString(), base,
                    ref.getProperty()));
        }
        return false;
    }

    protected List<ConstraintViolation> doValidate(FacesContext context, ValueReference vref, ValueExpression e,
            Object value) {
        DocumentValidationService s = Framework.getService(DocumentValidationService.class);
        DocumentValidationReport report = null;
        XPathAndField field = resolveField(context, vref, e);
        if (field != null) {
            boolean validateSubs = getHandleSubProperties().booleanValue();
            // use the xpath to validate the field
            // this allow to get the custom message defined for field if there's error
            report = s.validate(field.xpath, value, validateSubs);
            if (log.isDebugEnabled()) {
                log.debug(String.format("VALIDATED  value '%s' for expression '%s', base=%s, prop=%s", value,
                        e.getExpressionString(), vref.getBase(), vref.getProperty()));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("NOT Validating  value '%s' for expression '%s', base=%s, prop=%s", value,
                        e.getExpressionString(), vref.getBase(), vref.getProperty()));
            }
        }

        if (report != null && report.hasError()) {
            return report.asList();
        }

        return null;
    }

    private class XPathAndField {

        private Field field;

        private String xpath;

        public XPathAndField(Field field, String xpath) {
            super();
            this.field = field;
            this.xpath = xpath;
        }

    }

    protected XPathAndField resolveField(FacesContext context, ValueReference vref, ValueExpression ve) {
        Object base = vref.getBase();
        Object propObj = vref.getProperty();
        if (propObj != null && !(propObj instanceof String)) {
            // ignore cases where prop would not be a String
            return null;
        }
        String xpath = null;
        Field field = null;
        String prop = (String) propObj;
        Class<?> baseClass = base.getClass();
        if (DocumentPropertyContext.class.isAssignableFrom(baseClass)) {
            DocumentPropertyContext dc = (DocumentPropertyContext) base;
            xpath = dc.getSchema() + ":" + prop;
            field = getField(xpath);
        } else if (Property.class.isAssignableFrom(baseClass)) {
            xpath = ((Property) base).getPath() + "/" + prop;
            field = getField(((Property) base).getField(), prop);
        } else if (ProtectedEditableModel.class.isAssignableFrom(baseClass)) {
            ProtectedEditableModel model = (ProtectedEditableModel) base;
            ValueExpression listVe = model.getBinding();
            ValueExpressionAnalyzer expressionAnalyzer = new ValueExpressionAnalyzer(listVe);
            ValueReference listRef = expressionAnalyzer.getReference(context.getELContext());
            if (isResolvable(listRef, listVe)) {
                XPathAndField parentField = resolveField(context, listRef, listVe);
                if (parentField != null) {
                    field = getField(parentField.field, "*");
                    if (parentField.xpath == null) {
                        xpath = field.getName().getLocalName();
                    } else {
                        xpath = parentField.xpath + "/" + field.getName().getLocalName();
                    }
                }
            }
        } else if (ListItemMapper.class.isAssignableFrom(baseClass)) {
            ListItemMapper mapper = (ListItemMapper) base;
            ProtectedEditableModel model = mapper.getModel();
            ValueExpression listVe;
            if (model.getParent() != null) {
                // move one level up to resolve parent list binding
                listVe = model.getParent().getBinding();
            } else {
                listVe = model.getBinding();
            }
            ValueExpressionAnalyzer expressionAnalyzer = new ValueExpressionAnalyzer(listVe);
            ValueReference listRef = expressionAnalyzer.getReference(context.getELContext());
            if (isResolvable(listRef, listVe)) {
                XPathAndField parentField = resolveField(context, listRef, listVe);
                if (parentField != null) {
                    field = getField(parentField.field, prop);
                    if (field == null || field.getName() == null) {
                        // it should not happen but still, just in case
                        return null;
                    }
                    if (parentField.xpath == null) {
                        xpath = field.getName().getLocalName();
                    } else {
                        xpath = parentField.xpath + "/" + field.getName().getLocalName();
                    }
                }
            }
        } else {
            log.error(String.format("Cannot validate expression '%s, base=%s'", ve.getExpressionString(), base));
        }
        // cleanup / on begin or at end
        if (xpath != null) {
           xpath = StringUtils.strip(xpath, "/");
        } else if (field == null && xpath == null) {
           return null;
        }
        return new XPathAndField(field, xpath);
    }

    protected Field getField(Field field, String subName) {
        SchemaManager tm = Framework.getService(SchemaManager.class);
        return tm.getField(field, subName);
    }

    protected Field getField(String propertyName) {
        SchemaManager tm = Framework.getService(SchemaManager.class);
        return tm.getField(propertyName);
    }

    public Boolean getHandleSubProperties() {
        return handleSubProperties != null ? handleSubProperties : Boolean.TRUE;
    }

    public void setHandleSubProperties(Boolean handleSubProperties) {
        clearInitialState();
        this.handleSubProperties = handleSubProperties;
    }

    @Override
    public Object saveState(FacesContext context) {
        if (context == null) {
            throw new NullPointerException();
        }
        if (!initialStateMarked()) {
            Object values[] = new Object[1];
            values[0] = handleSubProperties;
            return (values);
        }
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        if (context == null) {
            throw new NullPointerException();
        }
        if (state != null) {
            Object values[] = (Object[]) state;
            handleSubProperties = (Boolean) values[0];
        }
    }

    @Override
    public boolean isTransient() {
        return transientValue;
    }

    @Override
    public void setTransient(boolean newTransientValue) {
        this.transientValue = newTransientValue;
    }

    @Override
    public void markInitialState() {
        initialState = true;
    }

    @Override
    public boolean initialStateMarked() {
        return initialState;
    }

    @Override
    public void clearInitialState() {
        initialState = false;
    }

}