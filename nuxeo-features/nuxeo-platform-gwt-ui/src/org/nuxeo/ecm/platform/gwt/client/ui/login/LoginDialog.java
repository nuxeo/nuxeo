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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client.ui.login;

import org.nuxeo.ecm.platform.gwt.client.http.HttpResponse;

import com.google.gwt.user.client.ui.Widget;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.form.validator.Validator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LoginDialog extends Window {

 // used to avoid open twice the login dialog - this may happend when dbl clicking in the tree
 // nav without having rights (because of the list nav)
    protected boolean isRunning = false;
    protected TextItem userName;
    protected PasswordItem password;
    protected String header;
    protected DynamicForm form;
    protected boolean isError = false;

    public LoginDialog() {
        this ("Authentication Required!");
    }

    public LoginDialog(String header) {
        super ();
        this.header = header;
        setAnimateMinimize(true);
        setWidth(300);
        setHeight(250);
        setTitle("Login");
        setShowMinimizeButton(false);
        setIsModal(true);
        setAutoCenter(true);
        addItem(createContent());
    }

    @Override
    public void show() {
        if(isRunning) {
            return;
        }
        isRunning = true;
        super.show();
        System.out.println(getZIndex());
    }

    /**
     * @return the isRunning.
     */
    public boolean isRunning() {
        return isRunning;
    }

    protected Widget createContent() {
        form = new DynamicForm();
        form.setAutoFocus(true);
        form.setNumCols(2);

        HeaderItem hItem =new HeaderItem("header");
        hItem.setValue("<h3>"+header+"</h3>");
        hItem.setAlign(Alignment.CENTER);

        userName = new TextItem("username");
        userName.setTitle("Username");
        userName.setSelectOnFocus(true);
        userName.setWrapTitle(false);

        password = new PasswordItem("password");
        password.setTitle("Password");
        password.setWrapTitle(false);

        SubmitItem submit = new SubmitItem("submit");
        submit.setTitle("Login");
        submit.setColSpan(2);
        submit.setAlign(Alignment.CENTER);

        submit.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (userName.getValue() == null) {
                    com.google.gwt.user.client.Window.alert("Fill the username please");
                    return;
                }
                if (password.getValue() == null) {
                    password.setValue("");
                }
                new MyLoginCommand(userName.getValue().toString().trim(), password.getValue().toString()).execute();
            }
        });


        StaticTextItem error = new StaticTextItem("error");
        Validator v = new CustomValidator() {
            @Override
         protected boolean condition(Object value) {
             return !isError;
         }
         };
         v.setErrorMessage("Authentication Failure. Try Again!");
         error.setValidators(v);
         error.setShowTitle(false);
         error.setColSpan(2);

        form.setFields(hItem, userName, password, submit, error);
        form.setPadding(4);
        form.setCellSpacing(4);


        return form;
    }

    class MyLoginCommand extends LoginCommand {
        public MyLoginCommand(String username, String password) {
            super (username, password);
        }
        @Override
        public void execute() {
            isError = false;
            form.validate(true); // hide error msg if any
            super.execute();
        }
        @Override
        public void onSuccess(HttpResponse response) {
            super.onSuccess(response);
            destroy();
        }
        @Override
        public void onFailure(Throwable cause) {
            isError = true;
            form.validate(true);
        }
    }

    @Override
    protected void onDestroy() {
        isRunning = false;
    }
}
