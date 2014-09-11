/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.SortInfo;

import com.sun.faces.util.MessageFactory;

/**
 * Validator for a list of {@link SortInfo} elements, checking that there is no
 * conflicting sort information (several sorts on same criterion)
 *
 * @author Anahide Tchertchian
 */
public class SortInfoListValidator implements Validator {

    public static final String VALIDATOR_ID = "SortInfoListValidator";

    /**
     * The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if the value
     * to validate is not a list of sort infos.
     */
    public static final String INVALID_VALUE_MESSAGE_ID = "error.sortInfoValidator.invalidValue";

    /**
     * The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if the value
     * to validate is a list of sort infos with conflicting criteria (several
     * sorts on the same criterion).
     * <p>
     * The message format string for this message may optionally include the
     * following placeholders:
     * <ul>
     * <li><code>{0}</code> replaced by the first found duplicate criterion.</li>
     * </ul>
     */
    public static final String CONFLICTING_CRITERIA_MESSAGE_ID = "error.sortInfoValidator.conflictingCriteria";

    /**
     * The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if the value
     * to validate contains a sort info with an empty sort criterion.
     */
    public static final String EMPTY_CRITERION_MESSAGE_ID = "error.sortInfoValidator.emptyCriterion";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {
        if (context == null || component == null) {
            throw new IllegalArgumentException();
        }
        if (value != null) {
            if (value instanceof List) {
                try {
                    List sortInfos = (List) value;
                    List<String> criteria = new ArrayList<String>();
                    for (Object sortInfo : sortInfos) {
                        String criterion = null;
                        if (sortInfo instanceof SortInfo) {
                            criterion = ((SortInfo) sortInfo).getSortColumn();
                        } else {
                            // assume it's a map
                            SortInfo sortInfoValue = SortInfo.asSortInfo((Map) sortInfo);
                            if (sortInfoValue == null) {
                                throw new ValidatorException(
                                        MessageFactory.getMessage(context,
                                                INVALID_VALUE_MESSAGE_ID));
                            }
                            criterion = sortInfoValue.getSortColumn();
                        }
                        if (criterion == null
                                || StringUtils.isEmpty(criterion.trim())) {
                            throw new ValidatorException(
                                    MessageFactory.getMessage(context,
                                            EMPTY_CRITERION_MESSAGE_ID));
                        }
                        if (criteria.contains(criterion)) {
                            throw new ValidatorException(
                                    MessageFactory.getMessage(context,
                                            CONFLICTING_CRITERIA_MESSAGE_ID,
                                            criterion));
                        } else {
                            criteria.add(criterion);
                        }
                    }
                } catch (ClassCastException e) {
                    throw new ValidatorException(MessageFactory.getMessage(
                            context, INVALID_VALUE_MESSAGE_ID));
                }
            }
        }
    }
}
