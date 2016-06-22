/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *     Sean Radford
 *
 * $Id: ComponentUtils.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectMany;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.ui.web.component.list.UIEditableList;
import org.nuxeo.runtime.api.Framework;

/**
 * Generic component helper methods.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public final class ComponentUtils {

    public static final String WHITE_SPACE_CHARACTER = "&#x0020;";

    private static final Log log = LogFactory.getLog(ComponentUtils.class);

    public static final String FORCE_NO_CACHE_ON_MSIE = "org.nuxeo.download.force.nocache.msie";

    // Utility class.
    private ComponentUtils() {
    }

    /**
     * Calls a component encodeBegin/encodeChildren/encodeEnd methods.
     */
    public static void encodeComponent(FacesContext context, UIComponent component) throws IOException {
        component.encodeBegin(context);
        component.encodeChildren(context);
        component.encodeEnd(context);
    }

    /**
     * Helper method meant to be called in the component constructor.
     * <p>
     * When adding sub components dynamically, the tree fetching could be a problem so all possible sub components must
     * be added.
     * <p>
     * Since 6.0, does not mark component as not rendered anymore, calls
     * {@link #hookSubComponent(FacesContext, UIComponent, UIComponent, String)} directly.
     *
     * @param parent
     * @param child
     * @param facetName facet name to put the child in.
     */
    public static void initiateSubComponent(UIComponent parent, String facetName, UIComponent child) {
        parent.getFacets().put(facetName, child);
        hookSubComponent(null, parent, child, facetName);
    }

    /**
     * Add a sub component to a UI component.
     * <p>
     * Since 6.0, does not the set the component as rendered anymore.
     *
     * @param context
     * @param parent
     * @param child
     * @param defaultChildId
     * @return child comp
     */
    public static UIComponent hookSubComponent(FacesContext context, UIComponent parent, UIComponent child,
            String defaultChildId) {
        // build a valid id using the parent id so that it's found everytime.
        String childId = child.getId();
        if (defaultChildId != null) {
            // override with default
            childId = defaultChildId;
        }
        // make sure it's set
        if (childId == null) {
            childId = context.getViewRoot().createUniqueId();
        }
        // reset client id
        child.setId(childId);
        child.setParent(parent);
        return child;
    }

    /**
     * Copies attributes and value expressions with given name from parent component to child component.
     */
    public static void copyValues(UIComponent parent, UIComponent child, String[] valueNames) {
        Map<String, Object> parentAttributes = parent.getAttributes();
        Map<String, Object> childAttributes = child.getAttributes();
        for (String name : valueNames) {
            // attributes
            if (parentAttributes.containsKey(name)) {
                childAttributes.put(name, parentAttributes.get(name));
            }
            // value expressions
            ValueExpression ve = parent.getValueExpression(name);
            if (ve != null) {
                child.setValueExpression(name, ve);
            }
        }
    }

    public static void copyLinkValues(UIComponent parent, UIComponent child) {
        String[] valueNames = { "accesskey", "charset", "coords", "dir", "disabled", "hreflang", "lang", "onblur",
                "onclick", "ondblclick", "onfocus", "onkeydown", "onkeypress", "onkeyup", "onmousedown", "onmousemove",
                "onmouseout", "onmouseover", "onmouseup", "rel", "rev", "shape", "style", "styleClass", "tabindex",
                "target", "title", "type" };
        copyValues(parent, child, valueNames);
    }

    public static Object getAttributeValue(UIComponent component, String attributeName, Object defaultValue) {
        return getAttributeValue(component, attributeName, Object.class, defaultValue, false);
    }

    /**
     * @since 8.2
     */
    public static <T> T getAttributeValue(UIComponent component, String name, Class<T> klass, T defaultValue,
            boolean required) {
        Object value = component.getAttributes().get(name);
        if (value == null) {
            value = defaultValue;
        }
        if (required && value == null) {
            throw new IllegalArgumentException("Component attribute with name '" + name + "' cannot be null: " + value);
        }
        if (value == null || value.getClass().isAssignableFrom(klass)) {
            return (T) value;
        }
        throw new IllegalArgumentException(
                "Component attribute with name '" + name + "' is not a " + klass + ": " + value);
    }

    public static Object getAttributeOrExpressionValue(FacesContext context, UIComponent component,
            String attributeName, Object defaultValue) {
        Object value = component.getAttributes().get(attributeName);
        if (value == null) {
            ValueExpression schemaExpr = component.getValueExpression(attributeName);
            value = schemaExpr.getValue(context.getELContext());
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Downloads a blob and sends it to the requesting user, in the JSF current context.
     *
     * @param doc the document, if available
     * @param xpath the blob's xpath or blobholder index, if available
     * @param blob the blob, if already fetched
     * @param filename the filename to use
     * @param reason the download reason
     * @since 7.3
     */
    public static void download(DocumentModel doc, String xpath, Blob blob, String filename, String reason) {
        download(doc, xpath, blob, filename, reason, null);
    }

    /**
     * Downloads a blob and sends it to the requesting user, in the JSF current context.
     *
     * @param doc the document, if available
     * @param xpath the blob's xpath or blobholder index, if available
     * @param blob the blob, if already fetched
     * @param filename the filename to use
     * @param reason the download reason
     * @param extendedInfos an optional map of extended informations to log
     * @since 7.3
     */
    public static void download(DocumentModel doc, String xpath, Blob blob, String filename, String reason,
            Map<String, Serializable> extendedInfos) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext.getResponseComplete()) {
            // nothing can be written, an error was probably already sent. don't bother
            log.debug("Cannot send " + filename + ", response already complete");
            return;
        }
        if (facesContext.getPartialViewContext().isAjaxRequest()) {
            // do not perform download in an ajax request
            return;
        }
        ExternalContext externalContext = facesContext.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        try {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(request, response, doc, xpath, blob, filename, reason, extendedInfos);
        } catch (IOException e) {
            log.error("Error while downloading the file: " + filename, e);
        } finally {
            facesContext.responseComplete();
        }
    }

    public static String downloadFile(File file, String filename, String reason) throws IOException {
        Blob blob = Blobs.createBlob(file);
        download(null, null, blob, filename, reason);
        return null;
    }

    /**
     * @deprecated since 7.3, use {@link #downloadFile(Blob, String)} instead
     */
    @Deprecated
    public static String download(FacesContext faces, Blob blob, String filename) {
        download(null, null, blob, filename, "download");
        return null;
    }

    /**
     * @deprecated since 7.3, use {@link #downloadFile(File, String)} instead
     */
    @Deprecated
    public static String downloadFile(FacesContext faces, String filename, File file) throws IOException {
        return downloadFile(file, filename, null);
    }

    protected static boolean forceNoCacheOnMSIE() {
        // see NXP-7759
        return Framework.isBooleanPropertyTrue(FORCE_NO_CACHE_ON_MSIE);
    }

    // hook translation passing faces context

    public static String translate(FacesContext context, String messageId) {
        return translate(context, messageId, (Object[]) null);
    }

    public static String translate(FacesContext context, String messageId, Object... params) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        return I18NUtils.getMessageString(bundleName, messageId, evaluateParams(context, params), locale);
    }

    public static void addErrorMessage(FacesContext context, UIComponent component, String message) {
        addErrorMessage(context, component, message, null);
    }

    public static void addErrorMessage(FacesContext context, UIComponent component, String message, Object[] params) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        message = I18NUtils.getMessageString(bundleName, message, evaluateParams(context, params), locale);
        FacesMessage msg = new FacesMessage(message);
        msg.setSeverity(FacesMessage.SEVERITY_ERROR);
        context.addMessage(component.getClientId(context), msg);
    }

    /**
     * Evaluates parameters to pass to translation methods if they are value expressions.
     *
     * @since 5.7
     */
    protected static Object[] evaluateParams(FacesContext context, Object[] params) {
        if (params == null) {
            return null;
        }
        Object[] res = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Object val = params[i];
            if (val instanceof String && ComponentTagUtils.isValueReference((String) val)) {
                ValueExpression ve = context.getApplication().getExpressionFactory().createValueExpression(
                        context.getELContext(), (String) val, Object.class);
                res[i] = ve.getValue(context.getELContext());
            } else {
                res[i] = val;
            }
        }
        return res;
    }

    /**
     * Gets the base naming container from anchor.
     * <p>
     * Gets out of suggestion box as it's a naming container and we can't get components out of it with a relative path
     * => take above first found container.
     *
     * @since 5.3.1
     */
    public static UIComponent getBase(UIComponent anchor) {
        // init base to given component in case there's no naming container for it
        UIComponent base = anchor;
        UIComponent container = anchor.getNamingContainer();
        if (container != null) {
            UIComponent supContainer = container.getNamingContainer();
            if (supContainer != null) {
                container = supContainer;
            }
        }
        if (container != null) {
            base = container;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Resolved base '%s' for anchor '%s'", base.getId(), anchor.getId()));
        }
        return base;
    }

    /**
     * Returns the component specified by the {@code componentId} parameter from the {@code base} component.
     * <p>
     * Does not throw any exception if the component is not found, returns {@code null} instead.
     *
     * @since 5.4
     */
    @SuppressWarnings("unchecked")
    public static <T> T getComponent(UIComponent base, String componentId, Class<T> expectedComponentClass) {
        if (componentId == null) {
            log.error("Cannot retrieve component with a null id");
            return null;
        }
        UIComponent component = ComponentRenderUtils.getComponent(base, componentId);
        if (component == null) {
            log.error("Could not find component with id: " + componentId);
        } else {
            try {
                return (T) component;
            } catch (ClassCastException e) {
                log.error("Invalid component with id '" + componentId + "': " + component
                        + ", expected a component with interface " + expectedComponentClass);
            }
        }
        return null;
    }

    static void clearTargetList(UIEditableList targetList) {
        int rc = targetList.getRowCount();
        for (int i = 0; i < rc; i++) {
            targetList.removeValue(0);
        }
    }

    static void addToTargetList(UIEditableList targetList, SelectItem[] items) {
        for (int i = 0; i < items.length; i++) {
            targetList.addValue(items[i].getValue());
        }
    }

    /**
     * Move items up inside the target select
     */
    public static void shiftItemsUp(UISelectMany targetSelect, UISelectItems targetItems,
            UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        shiftUp(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    public static void shiftItemsDown(UISelectMany targetSelect, UISelectItems targetItems,
            UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        shiftDown(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    public static void shiftItemsFirst(UISelectMany targetSelect, UISelectItems targetItems,
            UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        all = shiftFirst(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    public static void shiftItemsLast(UISelectMany targetSelect, UISelectItems targetItems,
            UIEditableList hiddenTargetList) {
        String[] selected = (String[]) targetSelect.getSelectedValues();
        SelectItem[] all = (SelectItem[]) targetItems.getValue();
        if (selected == null) {
            // nothing to do
            return;
        }
        all = shiftLast(selected, all);
        targetItems.setValue(all);
        clearTargetList(hiddenTargetList);
        addToTargetList(hiddenTargetList, all);
    }

    /**
     * Make a new SelectItem[] with items whose ids belong to selected first, preserving inner ordering of selected and
     * its complement in all.
     * <p>
     * Again this assumes that selected is an ordered sub-list of all
     * </p>
     *
     * @param selected ids of selected items
     * @param all
     * @return
     */
    static SelectItem[] shiftFirst(String[] selected, SelectItem[] all) {
        SelectItem[] res = new SelectItem[all.length];
        int sl = selected.length;
        int i = 0;
        int j = sl;
        for (SelectItem item : all) {
            if (i < sl && item.getValue().toString().equals(selected[i])) {
                res[i++] = item;
            } else {
                res[j++] = item;
            }
        }
        return res;
    }

    /**
     * Make a new SelectItem[] with items whose ids belong to selected last, preserving inner ordering of selected and
     * its complement in all.
     * <p>
     * Again this assumes that selected is an ordered sub-list of all
     * </p>
     *
     * @param selected ids of selected items
     * @param all
     * @return
     */
    static SelectItem[] shiftLast(String[] selected, SelectItem[] all) {
        SelectItem[] res = new SelectItem[all.length];
        int sl = selected.length;
        int cut = all.length - sl;
        int i = 0;
        int j = 0;
        for (SelectItem item : all) {
            if (i < sl && item.getValue().toString().equals(selected[i])) {
                res[cut + i++] = item;
            } else {
                res[j++] = item;
            }
        }
        return res;
    }

    static void swap(Object[] ar, int i, int j) {
        Object t = ar[i];
        ar[i] = ar[j];
        ar[j] = t;
    }

    static void shiftUp(String[] selected, SelectItem[] all) {
        int pos = -1;
        for (int i = 0; i < selected.length; i++) {
            String s = selected[i];
            // "pos" is the index of previous "s"
            int previous = pos;
            while (!all[++pos].getValue().equals(s)) {
            }
            // now current "s" is at "pos" index
            if (pos > previous + 1) {
                swap(all, pos, --pos);
            }
        }
    }

    static void shiftDown(String[] selected, SelectItem[] all) {
        int pos = all.length;
        for (int i = selected.length - 1; i >= 0; i--) {
            String s = selected[i];
            // "pos" is the index of previous "s"
            int previous = pos;
            while (!all[--pos].getValue().equals(s)) {
            }
            // now current "s" is at "pos" index
            if (pos < previous - 1) {
                swap(all, pos, ++pos);
            }
        }
    }

    /**
     * Move items from components to others.
     */
    public static void moveItems(UISelectMany sourceSelect, UISelectItems sourceItems, UISelectItems targetItems,
            UIEditableList hiddenTargetList, boolean setTargetIds) {
        String[] selected = (String[]) sourceSelect.getSelectedValues();
        if (selected == null) {
            // nothing to do
            return;
        }
        List<String> selectedList = Arrays.asList(selected);

        SelectItem[] all = (SelectItem[]) sourceItems.getValue();
        List<SelectItem> toMove = new ArrayList<SelectItem>();
        List<SelectItem> toKeep = new ArrayList<SelectItem>();
        List<String> hiddenIds = new ArrayList<String>();
        if (all != null) {
            for (SelectItem item : all) {
                String itemId = item.getValue().toString();
                if (selectedList.contains(itemId)) {
                    toMove.add(item);
                } else {
                    toKeep.add(item);
                    if (!setTargetIds) {
                        hiddenIds.add(itemId);
                    }
                }
            }
        }
        // reset left values
        sourceItems.setValue(toKeep.toArray(new SelectItem[] {}));
        sourceSelect.setSelectedValues(new Object[0]);

        // change right values
        List<SelectItem> newSelectItems = new ArrayList<SelectItem>();
        SelectItem[] oldSelectItems = (SelectItem[]) targetItems.getValue();
        if (oldSelectItems == null) {
            newSelectItems.addAll(toMove);
        } else {
            newSelectItems.addAll(Arrays.asList(oldSelectItems));
            List<String> oldIds = new ArrayList<String>();
            for (SelectItem oldItem : oldSelectItems) {
                String id = oldItem.getValue().toString();
                oldIds.add(id);
            }
            if (setTargetIds) {
                hiddenIds.addAll(0, oldIds);
            }
            for (SelectItem toMoveItem : toMove) {
                String id = toMoveItem.getValue().toString();
                if (!oldIds.contains(id)) {
                    newSelectItems.add(toMoveItem);
                    if (setTargetIds) {
                        hiddenIds.add(id);
                    }
                }
            }
        }
        targetItems.setValue(newSelectItems.toArray(new SelectItem[] {}));

        // update hidden values
        int numValues = hiddenTargetList.getRowCount();
        if (numValues > 0) {
            for (int i = numValues - 1; i > -1; i--) {
                hiddenTargetList.removeValue(i);
            }
        }
        for (String newHiddenValue : hiddenIds) {
            hiddenTargetList.addValue(newHiddenValue);
        }
    }

    /**
     * Move items from components to others.
     */
    public static void moveAllItems(UISelectItems sourceItems, UISelectItems targetItems,
            UIEditableList hiddenTargetList, boolean setTargetIds) {
        SelectItem[] all = (SelectItem[]) sourceItems.getValue();
        List<SelectItem> toMove = new ArrayList<SelectItem>();
        List<SelectItem> toKeep = new ArrayList<SelectItem>();
        List<String> hiddenIds = new ArrayList<String>();
        if (all != null) {
            for (SelectItem item : all) {
                if (!item.isDisabled()) {
                    toMove.add(item);
                } else {
                    toKeep.add(item);
                }
            }
        }
        // reset left values
        sourceItems.setValue(toKeep.toArray(new SelectItem[] {}));

        // change right values
        List<SelectItem> newSelectItems = new ArrayList<SelectItem>();
        SelectItem[] oldSelectItems = (SelectItem[]) targetItems.getValue();
        if (oldSelectItems == null) {
            newSelectItems.addAll(toMove);
        } else {
            newSelectItems.addAll(Arrays.asList(oldSelectItems));
            List<String> oldIds = new ArrayList<String>();
            for (SelectItem oldItem : oldSelectItems) {
                String id = oldItem.getValue().toString();
                oldIds.add(id);
            }
            if (setTargetIds) {
                hiddenIds.addAll(0, oldIds);
            }
            for (SelectItem toMoveItem : toMove) {
                String id = toMoveItem.getValue().toString();
                if (!oldIds.contains(id)) {
                    newSelectItems.add(toMoveItem);
                    if (setTargetIds) {
                        hiddenIds.add(id);
                    }
                }
            }
        }
        targetItems.setValue(newSelectItems.toArray(new SelectItem[] {}));

        // update hidden values
        int numValues = hiddenTargetList.getRowCount();
        if (numValues > 0) {
            for (int i = numValues - 1; i > -1; i--) {
                hiddenTargetList.removeValue(i);
            }
        }
        for (String newHiddenValue : hiddenIds) {
            hiddenTargetList.addValue(newHiddenValue);
        }
    }

    public static String verifyTarget(String toVerify, String defaultTarget) {
        if (StringUtils.isBlank(toVerify)) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        boolean ajaxRequest = context.getPartialViewContext().isAjaxRequest();
        if (ajaxRequest) {
            // ease up ajax re-rendering in case of js scripts parsing defer
            return null;
        }
        return defaultTarget;
    }

    public static String NUXEO_RESOURCE_RELOCATED = "NUXEO_RESOURCE_RELOCATED_MARKER";

    /**
     * Marks given component as relocated, so that subsequent calls to {@link #isRelocated(UIComponent)} returns true.
     *
     * @since 8.1
     */
    public static void setRelocated(UIComponent component) {
        component.getAttributes().put(NUXEO_RESOURCE_RELOCATED, "true");
    }

    /**
     * Returns true if given component is marked as relocated.
     *
     * @see #setRelocated(UIComponent)
     * @see #relocate(UIComponent, String, String)
     * @since 8.1
     */
    public static boolean isRelocated(UIComponent component) {
        return component.getAttributes().containsKey(NUXEO_RESOURCE_RELOCATED);
    }

    /**
     * Relocates given component, adding it to the view root resources for given target.
     * <p>
     * If given composite key is not null, current composite component client id is saved using this key on the
     * component attributes, for later reuse.
     * <p>
     * Component is also marked as relocated so that subsequent calls to {@link #isRelocated(UIComponent)} returns true.
     *
     * @since 8.1
     */
    public static void relocate(UIComponent component, String target, String compositeKey) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (compositeKey != null) {
            // We're checking for a composite component here as if the resource
            // is relocated, it may still require it's composite component context
            // in order to properly render. Store it for later use by
            // encodeBegin() and encodeEnd().
            UIComponent cc = UIComponent.getCurrentCompositeComponent(context);
            if (cc != null) {
                component.getAttributes().put(compositeKey, cc.getClientId(context));
            }
        }
        // avoid relocating resources that are not actually rendered
        if (isRendered(component)) {
            setRelocated(component);
            context.getViewRoot().addComponentResource(context, component, target);
        }
    }

    protected static boolean isRendered(UIComponent component) {
        UIComponent comp = component;
        while (comp.isRendered()) {
            UIComponent parent = comp.getParent();
            if (parent == null) {
                // reached root
                return true;
            } else {
                comp = parent;
            }
        }
        return false;
    }

}
