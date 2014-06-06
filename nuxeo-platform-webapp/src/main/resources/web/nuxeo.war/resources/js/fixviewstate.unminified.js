/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
var OmniFaces = OmniFaces || {};

/**
 * <p>Fix JSF view state if necessary. In Mojarra, it get lost on certain forms during certain ajax updates (e.g. 
 * updating content which in turn contains another form). When this script is loaded <em>after</em> the standard jsf.js
 * script containing standard JSF ajax API, then it will be automatically applied on all JSF ajax requests.
 * <pre>
 * &lt;h:outputScript library="javax.faces" name="jsf.js" target="head" /&gt;
 * &lt;h:outputScript library="omnifaces" name="fixviewstate.js" target="head" /&gt;
 * </pre>
 * <p>This script also recognizes jQuery ajax API as used by some component libraries such as PrimeFaces, it will then
 * be automatically applied on all jQuery ajax requests.
 * <pre>
 * &lt;h:outputScript library="primefaces" name="jquery/jquery.js" target="head" /&gt;
 * &lt;h:outputScript library="omnifaces" name="fixviewstate.js" target="head" /&gt;
 * </pre>
 * <p>Explicit declaration of jsf.js or jquery.js is not necessary. In that case you need to put the 
 * <code>&lt;h:outputScript&gt;</code> tag inside the <code>&lt;h:body&gt;</code> to ensure that it's loaded 
 * <em>after</em> the JSF and/or jQuery script.
 * <p>In case your JSF component library doesn't utilize standard JSF nor jQuery ajax API, but a proprietary one, and
 * exposes the missing view state problem, then you can still apply this script manually during the "complete" event of
 * the ajax request whereby the concrete <code>XMLHttpRequest</code> instance is available as some argument as follows:
 * <pre>
 * function someOncompleteCallbackFunction(xhr) {
 *     OmniFaces.FixViewState.apply(xhr.responseXML);
 * });
 * </pre>
 * <p>This was scheduled to be fixed in JSF 2.2 spec, however it was postponed to JSF 2.3. Note that this fix is not
 * necessary for MyFaces as they have internally already fixed it for long in their jsf.js.
 *
 * @author Bauke Scholtz
 * @link https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-790
 * @since 1.7
 */
OmniFaces.FixViewState = (function() {

  var fixViewState = {};
  var VIEW_STATE_PARAM = "javax.faces.ViewState";
  var VIEW_STATE_REGEX = new RegExp("^([\\w]+:)?" + VIEW_STATE_PARAM.replace(/\./g, "\\.") + "(:[0-9]+)?$");

  /**
   * Apply the "fix view state" on the current document based on the given XML response.
   */
  fixViewState.apply = function(responseXML) {
    if (typeof responseXML === "undefined") {
      return;
    }

    var viewStateValue = getViewStateValue(responseXML);

    if (!viewStateValue) {
      return;
    }

    for (var i = 0; i < document.forms.length; i++) {
      var form = document.forms[i];
      var viewStateElement = getViewStateElement(form);

      if (form.method == "post" && !viewStateElement) {
        // This POST form doesn't have a view state. This isn't right. Create it.
        createViewStateElement(form, viewStateValue);
      }
      else if (form.method == "get" && viewStateElement) {
        // PrimeFaces < 3.5.23 and < 4.0.7 also adds them to GET forms! This isn't right. Remove it.
        viewStateElement.parentNode.removeChild(viewStateElement);
      }
    }
  };

  /**
   * Get the view state value from the given XML response.
   */
  function getViewStateValue(responseXML) {
    var updates = responseXML.getElementsByTagName("update");

    for (var i = 0; i < updates.length; i++) {
      if (VIEW_STATE_REGEX.exec(updates[i].getAttribute("id"))) {
        return updates[i].firstChild.nodeValue;
      }
    }

    return null;
  }

  /**
   * Get the view state hidden input element from the given form.
   */
  function getViewStateElement(form) {
    var elements = form.elements;

    for (var i = 0; i < elements.length; i++) {
      if (elements[i].name == VIEW_STATE_PARAM) {
        return elements[i];
      }
    }

    return null;
  }

  /**
   * Create view state hidden input element with given view state value and add it to given form.
   */
  function createViewStateElement(form, viewStateValue) {
    var hidden;

    try {
      hidden = document.createElement("<input name='" + VIEW_STATE_PARAM + "'>"); // IE6-8.
    }
    catch(e) {
      hidden = document.createElement("input");
      hidden.setAttribute("name", VIEW_STATE_PARAM);
    }

    hidden.setAttribute("type", "hidden");
    hidden.setAttribute("value", viewStateValue);
    hidden.setAttribute("autocomplete", "off");
    form.appendChild(hidden);
  }

  return fixViewState;

})();

// Global initialization for standard JSF ajax API.
if (typeof jsf !== "undefined") {
  jsf.ajax.addOnEvent(function(data) {
    if (data.status == "success") {
      OmniFaces.FixViewState.apply(data.responseXML);
    }
  });
}

// Global initialization for jQuery ajax API.
if (typeof jQuery !== "undefined") {
  jQuery(document).ajaxComplete(function(event, xhr, options) {
    if (typeof xhr !== "undefined") {
      OmniFaces.FixViewState.apply(xhr.responseXML);
    }
  });
}