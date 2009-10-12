/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerEntryPoint;
import org.nuxeo.opensocial.container.client.bean.GadgetBean;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.core.Function;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.MessageBoxConfig;
import com.gwtext.client.widgets.Tool;

public class GadgetTools {

  private String ref;
  private GadgetForm form;

  public GadgetTools(String ref) {
    this.ref = ref;
  }

  public GadgetForm getGadgetForm() {
    return form;
  }

  public Tool[] getButtons(Boolean permission) {
    if (permission) {
      Tool gear = new Tool(Tool.GEAR, new Function() {
        public void execute() {
          form = new GadgetForm(ref);
          form.showForm();
        }
      });

      Tool close = new Tool(Tool.CLOSE, new Function() {
        public void execute() {
          MessageBox.show(new MessageBoxConfig() {
            {
              final GadgetBean gadget = ContainerEntryPoint.getContainerPortal()
                  .getGadgetPortlet(ref)
                  .getGadgetBean();

              setTitle("Delete a gadget");
              setMsg("Are you sure you want to delete '"
                  + gadget.getTitle() + "' gadget ?");
              setButtons(MessageBox.YESNO);
              setCallback(new MessageBox.PromptCallback() {
                public void execute(String btnID, String text) {
                  if ("yes".equals(btnID)) {
                    ContainerEntryPoint.getService()
                        .removeGadget(gadget, ContainerEntryPoint.getGwtParams(),
                            new AsyncCallback<GadgetBean>() {
                              public void onFailure(Throwable arg0) {
                                ContainerPortal.showErrorMessage("Error",
                                    "Error while deleting gadget");
                              }

                              public void onSuccess(GadgetBean gadget) {
                                ContainerEntryPoint.getContainerPortal()
                                    .removeGadgetPortlet(gadget);
                              }
                            });

                  }
                }
              });
            }
          });

        }
      });

      return new Tool[] { gear, close };
    }
    return new Tool[] {};
  }
}
