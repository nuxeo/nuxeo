package org.nuxeo.ecm.webapp.action;


import javax.faces.component.UISelectBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import static org.jboss.seam.ScopeType.STATELESS;;

/**
 * Performs re-rendering of webcontainer layout widgets.
 *
 * @author Anahide Tchertchian
 */

@Name("siteActions")
@Scope(STATELESS)
public class SiteActionsBean {

    private static final Log log = LogFactory.getLog(SiteActionsBean.class);

    public static final String SCHEMA_NAME = "webcontainer";

    public static final String ISWEBCONTAINER_PROPERTY_NAME = "isWebContainer";

    protected UISelectBoolean checkboxComponent;

    public UISelectBoolean getCheckboxComponent() {
        return checkboxComponent;
    }

    public void setCheckboxComponent(UISelectBoolean checkboxComponent) {
        this.checkboxComponent = checkboxComponent;
    }

    public boolean isWebContainerChecked() {
        Boolean checked = false;
        if (checkboxComponent != null) {
            UISelectBoolean checkbox = checkboxComponent;
            Object currentValue = checkbox.getSubmittedValue();
            if (currentValue == null) {
                currentValue = checkbox.getValue();
            }
            if (currentValue != null) {
                checked = Boolean.valueOf(currentValue.toString());
            }
        }
        return checked;
    }

}
