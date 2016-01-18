/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIInput;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Backing component for refactor of {@link UIInputFile} using a composite component.
 *
 * @since 8.2
 */
@FacesComponent("org.nuxeo.platform.ui.web.cc.inputFile")
public class UICompositeInputFile extends UIInput implements NamingContainer {

    /**
     * Returns the component family of {@link UINamingContainer}. (that's just required by composite component)
     */
    @Override
    public String getFamily() {
        return UINamingContainer.COMPONENT_FAMILY;
    }

    public List<String> getChoices() {
        return getAvailableChoices(null, false);
    }

    public String getOptionLabel(String choice) {
        String label = (String) ComponentUtils.getAttributeValue(this, choice + "Label", null);
        if (label == null) {
            label = ComponentUtils.translate(FacesContext.getCurrentInstance(), "label.inputFile." + choice + "Choice");
        }
        return label;
    }

    protected List<String> getAvailableChoices(Blob blob, boolean temp) {
        List<String> choices = new ArrayList<String>(3);
        boolean isRequired = isRequired();
        if (blob != null) {
            choices.add(temp ? InputFileChoice.KEEP_TEMP : InputFileChoice.KEEP);
        } else if (!isRequired) {
            choices.add(InputFileChoice.NONE);
        }
        boolean allowUpdate = true;
        if (blob != null) {
            BlobManager blobManager = Framework.getService(BlobManager.class);
            BlobProvider blobProvider = blobManager.getBlobProvider(blob);
            if (blobProvider != null && !blobProvider.supportsUserUpdate()) {
                allowUpdate = false;
            }
        }
        // if (allowUpdate) {
        // for (JSFBlobUploader uploader : uploaderService.getJSFBlobUploaders()) {
        // choices.add(uploader.getChoice());
        // }
        // if (blob != null && !isRequired) {
        // choices.add(InputFileChoice.DELETE);
        // }
        // }
        return choices;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        // NOOP
        // Calendar calendar = Calendar.getInstance();
        // int maxYear = getAttributeValue("maxyear", calendar.get(Calendar.YEAR));
        // int minYear = getAttributeValue("minyear", maxYear - 100);
        // Date date = (Date) getValue();
        //
        // if (date != null) {
        // calendar.setTime(date);
        // int year = calendar.get(Calendar.YEAR);
        //
        // if (year > maxYear || minYear > year) {
        // throw new IllegalArgumentException(
        // String.format("Year %d out of min/max range %d/%d.", year, minYear, maxYear));
        // }
        // }
        //
        // day.setValue(calendar.get(Calendar.DATE));
        // month.setValue(calendar.get(Calendar.MONTH) + 1);
        // year.setValue(calendar.get(Calendar.YEAR));
        super.encodeBegin(context);
    }

    /**
     * Return specified attribute value or otherwise the specified default if it's null.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getAttributeValue(String key, T defaultValue) {
        T value = (T) getAttributes().get(key);
        return (value != null) ? value : defaultValue;
    }
}
