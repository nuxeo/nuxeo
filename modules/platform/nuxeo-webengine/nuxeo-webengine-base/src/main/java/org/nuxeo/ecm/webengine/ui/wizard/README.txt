This package contains a wizard infrastructure for webengine.

See sample7 in webengine samples for an example of creating a wizard

The wizard is using the HTTP session to store its state and temporary data build by wizard pages.
It is automatically handling wizard page errors like validation errors by redisplaying the page and injecting
the error details in the template context as the variable ${This.error}.
The page template should check if this variable is set and display error messages accordingly.
Each page is driving how the form data is processed and what is the next page to display.
There are 4 types of buttons that a page template can use in their form: back, next, ok, cancel.
These buttons are made visible depending on the page style.

The back button is switching to the previous page if one exists.
The cancel button is leaving the wizard and redirects the browser to the caller of the wizard. Also the wizard session object is removed from the HTTP session.
The next button is posting the form data to the page handler to be processed which is returning the next page ID
on success or null if no next page is available. If a next page is available the wizard will redirect the browser to that page
otherwise it will call the performOk() method of the wizard to commit the wizard processing result.
The ok button is doing the same as the next button but it is never switching to the next page - it is always terminating the wizard by calling performOk().
After performOk is called the wizard session object is removed from the HTTP session.

Validation errors contains a map about which field failed to validate and what is the validation error message.


In order to create a wizard you have to follow these steps:

1. Create a WebObject that extends the Wizard class and implement the following methods:

  protected WizardPage<List<String>>[] createPages();

  This method is creating a list of pages to be used by the wizard. The first page in the array is the initial page.
  The next pages will be computed dynamically by each page when they are processing the submitted data.

  protected List<String> createData()

  This method is creating the initial data (usually an empty structure) that will be put in the HTTP session and that should be filled out by each page when processed.
  This object should be committed by the Wizard.performOk() when the wizard terminates.
  You can access this data in this method by calling: session.getData()

  protected Object performOk() throws WizardException

  This method is called when the wizard is done and session data should be committed.
  A typical implementation of this method will do something like this:

  DocumentModel doc = session.getData();
  try {
    coreSession.saveDocument(doc);
    coreSession.save();
  } catch (Throwable t) {
    throw new WizardException("some error", t);
  }

  You can control also any other aspect from the wizard class (like how the cancel is handled etc.) but this is not covered here.
  Anyway the default implementation provides a "natural" handling of most of these details.

2. Create an object extending WizardPage for each page you want to add to the wizard.

A wizard page must implement one method:

public abstract String process(WebContext ctx, FormData form, T data) throws WizardException;

This method is invoked when a page was submitted and let the page logic to handle input data (the FormData object) and to fill the session data (the data object) accordingly.
After the processing is done the method must return the next page ID in the wizard or null if there are no more pages.
The FormData handles also file uploads.

A typical implementation should do something like:

String title = form.getString("title");
if (title == null || title.length().trim().length() == 0) {
    WizardException e = new WizardException();
    e.addError("title", "Title cannot be empty");
    throw e;
}
data.setTitle(title);
return "next_page";

Apart the process method a page should specify a style. This style is controlling which buttons are visible in the page form.
You may use any combination of OK | CANCEL | BACK | NEXT
There are also some shortcuts for most used styles: INITIAL, MIDDLE, LAST

If you need dynamic enablement of these actions you need to explicitly implement this in the page template.

3. For each page you need to define a view (e.g. freemarker template).
The view files must be placed in the view directory of the wizard WebObject (e.g. skin/views/mywizard)
and must use as the name the ID of the corresponding page (e.g. page1.ftl if Page1.getId() is "page1")

The template may import the "lib/wizard.ftl" to be able to use default routines to show form buttons.
Example:

<#import "lib/wizard.ftl" as wiz>
This is page 1
<p>
<form method="POST">
Title: <input type="text" name="title" value="${This.session.data.title}">
<br>
<@wiz.back/>
<@wiz.cancel/>
<@wiz.ok/>
<@wiz.next/>
</form>

The buttons will be showed only if the page style enable them.
To be able to use "lib/wizard.ftl" your module must extends the "base" module.

4. To call the wizard you need to add a JAX-RS method that will create your wizard and bind it to a path. Example:

@WebObject(type="mysite")
public class WizardParent extends DefaultObject {

    @Path("wizard")
    public Object showWizard() {
        return newObject("mywizard");
    }

}

where mywizard is the type of your WebObject wizard.
