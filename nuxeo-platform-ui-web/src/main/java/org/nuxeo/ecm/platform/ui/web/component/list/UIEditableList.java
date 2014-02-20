/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * $Id: UIEditableList.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.list;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ContextCallback;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.platform.el.FieldAdapterManager;
import org.nuxeo.ecm.platform.ui.web.component.ResettableComponent;
import org.nuxeo.ecm.platform.ui.web.model.EditableModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.EditableModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.EditableModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.model.impl.ProtectedEditableModelImpl;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;

import com.sun.faces.facelets.tag.jsf.ComponentSupport;

/**
 * Editable table component.
 * <p>
 * Allows to add/remove elements from an {@link UIEditableList}, inspired from
 * Trinidad UIXCollection component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
// XXX AT: see if needs to manage row keys as Trinidad does in case multiple
// user edit the same list concurrently.
public class UIEditableList extends UIInput implements NamingContainer,
        ResettableComponent {

    public static final String COMPONENT_TYPE = UIEditableList.class.getName();

    public static final String COMPONENT_FAMILY = UIEditableList.class.getName();

    private static final Log log = LogFactory.getLog(UIEditableList.class);

    // use this key to indicate uninitialized state.
    private static final Object _NULL = new Object();

    protected String model = "";

    protected Object template;

    protected Boolean diff;

    protected Integer number;

    protected Boolean removeEmpty;

    private InternalState state;

    private static final class InternalState implements Serializable {

        private static final long serialVersionUID = 4730664880938551346L;

        private transient boolean _hasEvent = false;

        private transient Object _value;

        // this is true if this is the first request for this viewID and
        // processDecodes was not called:
        private transient boolean _isFirstRender = true;

        private transient boolean _isInitialized = false;

        // this is the rowKey used to retrieve the default stamp-state for all
        // rows:
        private transient Object _initialStampStateKey = _NULL;

        private EditableModel _model;

        private StampState _stampState;
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    // state management

    protected final InternalState getInternalState(boolean create) {
        if (state == null && create) {
            state = new InternalState();
        }
        return state;
    }

    protected final StampState getStampState() {
        InternalState iState = getInternalState(true);
        if (iState._stampState == null) {
            iState._stampState = new StampState();
        }
        return iState._stampState;
    }

    protected final void initializeState(final boolean force) {
        InternalState iState = getInternalState(true);
        if (!iState._isInitialized || force) {
            iState._isInitialized = true;
        }
    }

    @Override
    public Object saveState(FacesContext context) {
        // _stampState is stored as an instance variable, so it isn't
        // automatically saved
        Object superState = super.saveState(context);
        final Object stampState;
        final Object editableModel;

        // be careful not to create the internal state too early:
        // otherwise, the internal state will be shared between
        // nested table stamps:
        InternalState iState = getInternalState(false);
        if (iState != null) {
            stampState = iState._stampState;
            editableModel = iState._model;
        } else {
            stampState = null;
            editableModel = null;
        }

        if (superState != null || stampState != null) {
            return new Object[] { superState, stampState, getSubmittedValue(),
                    editableModel, model, template, diff, number, removeEmpty };
        }
        return null;
    }

    @Override
    public Object getValue() {
        Object value = super.getValue();
        if (value instanceof ListProperty) {
            try {
                value = ((ListProperty) value).getValue();
                value = FieldAdapterManager.getValueForDisplay(value);
            } catch (PropertyException e) {
            }
        }
        return value;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        final Object superState;
        final Object stampState;
        final Object submittedValue;
        final Object editableModel;
        Object[] array = (Object[]) state;
        if (array != null) {
            superState = array[0];
            stampState = array[1];
            submittedValue = array[2];
            editableModel = array[3];
            model = (String) array[4];
            template = array[5];
            diff = (Boolean) array[6];
            number = (Integer) array[7];
            removeEmpty = (Boolean) array[8];
        } else {
            superState = null;
            stampState = null;
            submittedValue = null;
            editableModel = null;
        }
        super.restoreState(context, superState);
        setSubmittedValue(submittedValue);

        if (stampState != null || model != null) {
            InternalState iState = getInternalState(true);
            iState._stampState = (StampState) stampState;
            iState._model = (EditableModel) editableModel;
        } else {
            // be careful not to force creation of the internal state
            // too early:
            InternalState iState = getInternalState(false);
            if (iState != null) {
                iState._stampState = (StampState) stampState;
                iState._model = (EditableModel) editableModel;
            }
        }
    }

    protected static boolean valueChanged(Object cached, Object current) {
        boolean changed = false;
        if (cached == null) {
            changed = (current != null);
        } else if (current == null) {
            changed = true;
        } else if (cached instanceof Object[] && current instanceof Object[]) {
            // arrays do not compare ok if reference is different, so match
            // each element
            Object[] cachedArray = (Object[]) cached;
            Object[] currentArray = (Object[]) current;
            if (cachedArray.length != currentArray.length) {
                return true;
            } else {
                for (int i = 0; i < cachedArray.length; i++) {
                    if (valueChanged(cachedArray[i], currentArray[i])) {
                        return true;
                    }
                }
            }
        } else if (cached instanceof List && current instanceof List) {
            // arrays do not compare ok if reference is different, so match
            // each element
            List cachedList = (List) cached;
            List currentList = (List) current;
            if (cachedList.size() != currentList.size()) {
                return true;
            } else {
                for (int i = 0; i < cachedList.size(); i++) {
                    if (valueChanged(cachedList.get(i), currentList.get(i))) {
                        return true;
                    }
                }
            }
        } else if (cached instanceof Map && current instanceof Map) {
            // arrays do not compare ok if reference is different, so match
            // each element
            Map cachedMap = (Map) cached;
            Map currentMap = (Map) current;
            if (cachedMap.size() != currentMap.size()) {
                return true;
            } else {
                for (Object key : cachedMap.keySet()) {
                    if (valueChanged(cachedMap.get(key), currentMap.get(key))) {
                        return true;
                    }
                }
            }
        } else {
            changed = !(cached.equals(current));
        }
        return changed;
    }

    protected void flushCachedModel() {
        InternalState iState = getInternalState(true);
        Object value = getValue();
        Object cachedValue = null;
        if (iState._model != null) {
            cachedValue = iState._model.getOriginalData();
        }
        if (valueChanged(cachedValue, value)) {
            iState._value = value;
            iState._model = createEditableModel(iState._model, value);
        }
    }

    /**
     * Resets the cache model
     * <p>
     * Can be useful when re-rendering a list with ajax and not wanting to keep
     * cached values already submitted.
     *
     * @since 5.3.1
     */
    public void resetCachedModel() {
        InternalState iState = getInternalState(true);
        Object value = getValue();
        iState._value = value;
        iState._model = createEditableModel(iState._model, value);
    }

    /**
     * Returns the value exposed in request map for the model name.
     * <p>
     * This is useful for restoring this value in the request map.
     *
     * @since 5.4.2
     */
    protected final Object saveRequestMapModelValue() {
        String modelName = getModel();
        if (modelName != null) {
            FacesContext context = getFacesContext();
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            if (requestMap.containsKey(modelName)) {
                return requestMap.get(modelName);
            }
        }
        return null;
    }

    /**
     * Restores the given value in the request map for the model name.
     *
     * @since 5.4.2
     */
    protected final void restoreRequestMapModelValue(Object value) {
        String modelName = getModel();
        if (modelName != null) {
            FacesContext context = getFacesContext();
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            if (value == null) {
                requestMap.remove(modelName);
            } else {
                requestMap.put(modelName, value);
            }
        }
    }

    /**
     * Prepares this component for a change in the rowData.
     * <p>
     * This method should be called right before the rowData changes. It saves
     * the internal states of all the stamps of this component so that they can
     * be restored when the rowData is reverted.
     */
    protected final void preRowDataChange() {
        // save stamp state
        StampState stampState = getStampState();
        FacesContext context = getFacesContext();
        Object currencyObj = getRowKey();
        int position = 0;
        for (UIComponent stamp : getChildren()) {
            if (stamp.isTransient()) {
                continue;
            }
            Object state = StampState.saveStampState(context, stamp);
            // String stampId = stamp.getId();
            // TODO
            // temporarily use position. later we need to use ID's to access
            // stamp state everywhere, and special case NamingContainers:
            String stampId = String.valueOf(position++);
            stampState.put(currencyObj, stampId, state);
        }
    }

    /**
     * Sets up this component to use the new rowData.
     * <p>
     * This method should be called right after the rowData changes. It sets up
     * the var EL variable to be the current rowData. It also sets up the
     * internal states of all the stamps of this component to match this new
     * rowData.
     */
    protected final void postRowDataChange() {
        StampState stampState = getStampState();
        FacesContext context = getFacesContext();
        Object currencyObj = getRowKey();

        // expose model to the request map or remove it given row availability
        String modelName = getModel();
        if (modelName != null) {
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            EditableModel model = getEditableModel();
            if (model == null || !model.isRowAvailable()) {
                requestMap.remove(modelName);
            } else {
                // only expose protected model
                requestMap.put(modelName, new ProtectedEditableModelImpl(model));
            }
        }

        int position = 0;
        for (UIComponent stamp : getChildren()) {
            if (stamp.isTransient()) {
                continue;
            }
            // String stampId = stamp.getId();
            // TODO
            // temporarily use position. later we need to use ID's to access
            // stamp state everywhere, and special case NamingContainers:
            String stampId = String.valueOf(position++);
            Object state = stampState.get(currencyObj, stampId);
            if (state == null) {
                Object iniStateObj = getCurrencyKeyForInitialStampState();
                state = stampState.get(iniStateObj, stampId);
                if (state == null) {
                    log.error("There was no initial stamp state for currencyKey:"
                            + currencyObj
                            + " and currencyKeyForInitialStampState:"
                            + iniStateObj + " and stampId:" + stampId);
                    continue;
                }
            }
            StampState.restoreStampState(context, stamp, state);
        }
    }

    /**
     * Gets the currencyObject to setup the rowData to use to build initial
     * stamp state.
     */
    protected Object getCurrencyKeyForInitialStampState() {
        InternalState iState = getInternalState(false);
        if (iState == null) {
            return null;
        }

        Object rowKey = iState._initialStampStateKey;
        return (rowKey == _NULL) ? null : rowKey;
    }

    // model management

    /**
     * Gets the EditableModel to use with this component.
     */
    public final EditableModel getEditableModel() {
        InternalState iState = getInternalState(true);
        if (iState._model == null) {
            initializeState(false);
            iState._value = getValue();
            iState._model = createEditableModel(null, iState._value);
            assert iState._model != null;
        }
        // model might not have been created if createIfNull is false:
        if ((iState._initialStampStateKey == _NULL) && (iState._model != null)) {
            // if we have not already initialized the initialStampStateKey
            // that means that we don't have any stamp-state to use as the
            // default
            // state for rows that we have not seen yet. So...
            // we will use any stamp-state for the initial rowKey on the model
            // as the default stamp-state for all rows:
            iState._initialStampStateKey = iState._model.getRowKey();
        }
        return iState._model;
    }

    /**
     * Returns a new EditableModel from given value.
     *
     * @param current the current CollectionModel, or null if there is none.
     * @param value this is the value returned from {@link #getValue()}
     */
    protected EditableModel createEditableModel(EditableModel current,
            Object value) {
        EditableModel model = new EditableModelImpl(value);
        Integer defaultNumber = getNumber();
        int missing = 0;
        if (defaultNumber != null) {
            missing = defaultNumber - model.size();
        }
        if (defaultNumber != null && missing > 0) {
            try {
                Object template = getTemplate();
                if (template instanceof Serializable) {
                    Serializable serializableTemplate = (Serializable) template;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(out);
                    oos.writeObject(serializableTemplate);
                    oos.close();
                    for (int i = 0; i < missing; i++) {
                        // deserialize to make sure it is not the same instance
                        byte[] pickled = out.toByteArray();
                        InputStream in = new ByteArrayInputStream(pickled);
                        ObjectInputStream ois = new ObjectInputStream(in);
                        Object newTemplate = ois.readObject();
                        model.addValue(newTemplate);
                    }
                } else {
                    log.warn("Template is not serializable, cannot clone "
                            + "to add unreferenced value into model.");
                    model.addValue(template);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        model.setRowIndex(-1);
        assert model.getRowIndex() == -1 : "RowIndex did not reset to -1";
        return model;
    }

    /**
     * Checks to see if the current row is available. This is useful when the
     * total number of rows is not known.
     *
     * @see EditableModel#isRowAvailable
     * @return true iff the current row is available.
     */
    public final boolean isRowAvailable() {
        return getEditableModel().isRowAvailable();
    }

    /**
     * Checks to see if the current row is modified.
     *
     * @see EditableModel#isRowModified
     * @return true iff the current row is modified.
     */
    public final boolean isRowModified() {
        return getEditableModel().isRowModified();
    }

    /**
     * Gets the total number of rows in this table.
     *
     * @see EditableModel#getRowCount
     * @return -1 if the total number is not known.
     */
    public final int getRowCount() {
        return getEditableModel().getRowCount();
    }

    /**
     * Gets the index of the current row.
     *
     * @see EditableModel#getRowIndex
     * @return -1 if the current row is unavailable.
     */
    public final int getRowIndex() {
        return getEditableModel().getRowIndex();
    }

    /**
     * Gets the rowKey of the current row.
     *
     * @see EditableModel#getRowKey
     * @return null if the current row is unavailable.
     */
    public final Integer getRowKey() {
        return getEditableModel().getRowKey();
    }

    /**
     * Gets the data for the current row.
     *
     * @see EditableModel#getRowData
     * @return null if the current row is unavailable
     */
    public final Object getRowData() {
        EditableModel model = getEditableModel();
        // we need to call isRowAvailable() here because the 1.0 sun RI was
        // throwing exceptions when getRowData() was called with rowIndex=-1
        return model.isRowAvailable() ? model.getRowData() : null;
    }

    /**
     * Makes a row current.
     * <p>
     * This method calls {@link #preRowDataChange} and
     * {@link #postRowDataChange} as appropriate.
     *
     * @see EditableModel#setRowIndex
     * @param rowIndex The rowIndex of the row that should be made current. Use
     *            -1 to clear the current row.
     */
    public void setRowIndex(int rowIndex) {
        preRowDataChange();
        getEditableModel().setRowIndex(rowIndex);
        postRowDataChange();
    }

    /**
     * Makes a row current.
     * <p>
     * This method calls {@link #preRowDataChange} and
     * {@link #postRowDataChange} as appropriate.
     *
     * @see EditableModel#setRowKey
     * @param rowKey The rowKey of the row that should be made current. Use
     *            null to clear the current row.
     */
    public void setRowKey(Integer rowKey) {
        // XXX AT: do not save state before setting row key as current index
        // may not point to the same object anymore (XXX: need to handle this
        // better, as events may change the data too, in which case we would
        // want the state to be saved).
        // preRowDataChange();
        getEditableModel().setRowKey(rowKey);
        postRowDataChange();
    }

    /**
     * Records a value modification.
     *
     * @see EditableModel#recordValueModified
     */
    public final void recordValueModified(int index, Object newValue) {
        getEditableModel().recordValueModified(index, newValue);
    }

    /**
     * Adds a value to the end of the editable model.
     *
     * @param value the value to add
     * @return true if value was added.
     */
    public boolean addValue(Object value) {
        return getEditableModel().addValue(value);
    }

    /**
     * Inserts value at given index on the editable model.
     *
     * @throws IllegalArgumentException if model does not handle this index.
     */
    public void insertValue(int index, Object value) {
        getEditableModel().insertValue(index, value);
    }

    /**
     * Modifies value at given index on the editable model.
     *
     * @return the old value at that index.
     * @throws IllegalArgumentException if model does not handle one of given
     *             indexes.
     */
    public Object moveValue(int fromIndex, int toIndex) {
        throw new NotImplementedException();
    }

    /**
     * Removes value at given index on the editable model.
     *
     * @return the old value at that index.
     * @throws IllegalArgumentException if model does not handle this index.
     */
    public Object removeValue(int index) {
        return getEditableModel().removeValue(index);
    }

    /**
     * Gets model name exposed in request map.
     */
    public String getModel() {
        if (model != null) {
            return model;
        }
        ValueExpression ve = getValueExpression("model");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Sets model name exposed in request map.
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Gets template to be used when adding new values to the model.
     */
    public Object getTemplate() {
        if (template != null) {
            return template;
        }
        ValueExpression ve = getValueExpression("template");
        if (ve != null) {
            try {
                Object res = ve.getValue(getFacesContext().getELContext());
                if (res instanceof String) {
                    // try to resolve a second time in case it's an expression
                    res = ComponentTagUtils.resolveElExpression(
                            getFacesContext(), (String) res);
                }
                return res;
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Sets template to be used when adding new values to the model.
     */
    public final void setTemplate(Object template) {
        this.template = template;
    }

    /**
     * Gets boolean stating if diff must be used when saving the value
     * submitted.
     */
    public Boolean getDiff() {
        if (diff != null) {
            return diff;
        }
        ValueExpression ve = getValueExpression("diff");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    /**
     * Sets boolean stating if diff must be used when saving the value
     * submitted.
     */
    public void setDiff(Boolean diff) {
        this.diff = diff;
    }

    public Integer getNumber() {
        if (number != null) {
            return number;
        }
        ValueExpression ve = getValueExpression("number");
        if (ve != null) {
            try {
                return ((Number) ve.getValue(getFacesContext().getELContext())).intValue();
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return null;
        }
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Boolean getRemoveEmpty() {
        if (removeEmpty != null) {
            return removeEmpty;
        }
        ValueExpression ve = getValueExpression("removeEmpty");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    public void setRemoveEmpty(Boolean removeEmpty) {
        this.removeEmpty = removeEmpty;
    }

    /**
     * Override container client id resolution to handle recursion.
     */
    @Override
    public String getContainerClientId(FacesContext context) {
        String id = super.getClientId(context);
        int index = getRowIndex();
        if (index != -1) {
            id += NamingContainer.SEPARATOR_CHAR + String.valueOf(index);
        }
        return id;
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    public void setRendererType(String rendererType) {
        // do nothing
    }

    @Override
    public final void encodeBegin(FacesContext context) throws IOException {
        initializeState(false);
        flushCachedModel();

        super.encodeBegin(context);
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        super.encodeEnd(context);
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    /**
     * Repeatedly render the children as many times as needed.
     */
    @Override
    public void encodeChildren(final FacesContext context) throws IOException {
        if (!isRendered()) {
            return;
        }

        processFacetsAndChildren(context, PhaseId.RENDER_RESPONSE);
    }

    // events
    /**
     * Delivers a wrapped event to the appropriate component. If the event is a
     * special wrapped event, it is unwrapped.
     *
     * @param event a FacesEvent
     * @throws javax.faces.event.AbortProcessingException
     */
    @Override
    public void broadcast(FacesEvent event) {
        if (event instanceof EditableModelRowEvent) {
            EditableModelRowEvent rowEvent = (EditableModelRowEvent) event;
            Integer old = getRowKey();
            Object requestMapValue = saveRequestMapModelValue();
            try {
                setRowKey(rowEvent.getKey());
                FacesEvent wrapped = rowEvent.getEvent();
                wrapped.getComponent().broadcast(wrapped);
                setRowKey(old);
            } finally {
                restoreRequestMapModelValue(requestMapValue);
            }
        } else {
            super.broadcast(event);
        }
    }

    /**
     * Queues an event. If there is a currency set on this table, then the
     * event will be wrapped so that when it is finally delivered, the correct
     * currency will be restored.
     *
     * @param event a FacesEvent
     */
    @Override
    public void queueEvent(FacesEvent event) {
        if (event.getSource() == this) {
            InternalState iState = getInternalState(true);
            iState._hasEvent = true;
        }

        // we want to wrap up the event so we can execute it in the correct
        // context (with the correct rowKey/rowData):
        Integer currencyKey = getRowKey();
        event = new EditableModelRowEvent(this, event, currencyKey);
        super.queueEvent(event);
    }

    @Override
    public void processDecodes(FacesContext context) {
        if (!isRendered()) {
            return;
        }

        initializeState(false);

        InternalState iState = getInternalState(true);
        iState._isFirstRender = false;

        flushCachedModel();

        // Make sure _hasEvent is false.
        iState._hasEvent = false;

        processFacetsAndChildren(context, PhaseId.APPLY_REQUEST_VALUES);
        decode(context);

        // XXX AT: cannot validate values because model is not updated yet
        // if (isImmediate()) {
        // executeValidate(context);
        // }
    }

    @Override
    public void processValidators(FacesContext context) {
        if (!isRendered()) {
            return;
        }

        initializeState(true);

        processFacetsAndChildren(context, PhaseId.PROCESS_VALIDATIONS);

        // XXX AT: cannot validate values because model is not updated yet
        // if (!isImmediate()) {
        // executeValidate(context);
        // }
    }

    @Override
    public void processUpdates(FacesContext context) {
        if (!isRendered()) {
            return;
        }

        initializeState(true);

        processFacetsAndChildren(context, PhaseId.UPDATE_MODEL_VALUES);

        EditableModel model = getEditableModel();
        if (model.isDirty()) {
            // remove empty values if needed
            Boolean removeEmpty = getRemoveEmpty();
            Object data = model.getWrappedData();
            Object template = getTemplate();
            if (removeEmpty && data instanceof List) {
                List dataList = (List) data;
                for (int i = dataList.size() - 1; i > -1; i--) {
                    Object item = dataList.get(i);
                    if (item == null || item.equals(template)) {
                        model.removeValue(i);
                    }
                }
            }
        }

        Object submitted = model.getWrappedData();
        if (submitted == null) {
            // set submitted to empty list to force validation
            submitted = Collections.emptyList();
        }
        setSubmittedValue(submitted);

        // execute validate now that value is submitted
        executeValidate(context);

        if (isValid() && isLocalValueSet()) {
            Boolean setDiff = getDiff();
            if (setDiff) {
                // set list diff instead of the whole list
                setValue(model.getListDiff());
            }
        }

        try {
            updateModel(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

        if (!isValid()) {
            context.renderResponse();
        } else {
            // force reset
            resetCachedModel();
        }
    }

    protected final void processFacetsAndChildren(final FacesContext context,
            final PhaseId phaseId) {
        Exception exception = null;
        List<UIComponent> stamps = getChildren();
        int oldIndex = getRowIndex();
        int end = getRowCount();
        Object requestMapValue = saveRequestMapModelValue();
        try {
            int first = 0;
            for (int i = first; i < end; i++) {
                setRowIndex(i);
                if (isRowAvailable()) {
                    for (UIComponent stamp : stamps) {
                        processComponent(context, stamp, phaseId);
                    }
                    if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                        // detect changes during process update phase and fill
                        // the EditableModel list diff.
                        if (isRowModified()) {
                            recordValueModified(i, getRowData());
                        }
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            exception = e;
        } finally {
            setRowIndex(oldIndex);
            restoreRequestMapModelValue(requestMapValue);
        }
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }
        }
    }

    protected final void processComponent(FacesContext context,
            UIComponent component, PhaseId phaseId) {
        if (component != null) {
            if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
                component.processDecodes(context);
            } else if (phaseId == PhaseId.PROCESS_VALIDATIONS) {
                component.processValidators(context);
            } else if (phaseId == PhaseId.UPDATE_MODEL_VALUES) {
                component.processUpdates(context);
            } else if (phaseId == PhaseId.RENDER_RESPONSE) {
                try {
                    ComponentSupport.encodeRecursive(context, component);
                } catch (IOException err) {
                    log.error("Error while rendering component " + component);
                }
            } else {
                throw new IllegalArgumentException("Bad PhaseId:" + phaseId);
            }
        }
    }

    private void executeValidate(FacesContext context) {
        try {
            validate(context);
        } catch (RuntimeException e) {
            context.renderResponse();
            throw e;
        }

        if (!isValid()) {
            context.renderResponse();
        }
    }

    @Override
    public boolean invokeOnComponent(FacesContext context, String clientId,
            ContextCallback callback) throws FacesException {
        if (null == context || null == clientId || null == callback) {
            throw new NullPointerException();
        }

        // try invoking on list
        String myId = super.getClientId(context);
        if (clientId.equals(myId)) {
            try {
                callback.invokeContextCallback(context, this);
                return true;
            } catch (Exception e) {
                throw new FacesException(e);
            }
        }

        Exception exception = null;
        List<UIComponent> stamps = getChildren();
        int oldIndex = getRowIndex();
        int end = getRowCount();
        boolean found = false;
        Object requestMapValue = saveRequestMapModelValue();
        try {
            int first = 0;
            for (int i = first; i < end; i++) {
                setRowIndex(i);
                if (isRowAvailable()) {
                    for (UIComponent stamp : stamps) {
                        found = stamp.invokeOnComponent(context, clientId,
                                callback);
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            exception = e;
        } finally {
            setRowIndex(oldIndex);
            restoreRequestMapModelValue(requestMapValue);
        }
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            }
        }

        return found;
    }

}
