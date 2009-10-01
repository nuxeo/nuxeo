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

              setTitle("Suppression de gadget");
              setMsg("Voulez-vous vraiment supprimer le gadget '"
                  + gadget.getTitle() + "' ?");
              setButtons(MessageBox.YESNO);
              setCallback(new MessageBox.PromptCallback() {
                public void execute(String btnID, String text) {
                  if ("yes".equals(btnID)) {
                    ContainerEntryPoint.getService()
                        .removeGadget(gadget, ContainerEntryPoint.getGwtParams(),
                            new AsyncCallback<GadgetBean>() {
                              public void onFailure(Throwable arg0) {
                                ContainerPortal.showErrorMessage("Erreur",
                                    "La suppression du gadget a échouée");
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
