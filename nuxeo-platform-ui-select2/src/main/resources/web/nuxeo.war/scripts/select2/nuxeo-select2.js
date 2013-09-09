(function() {

  function userEntryDefaultFormatter(entry) {
    var markup = "";
    if (entry.type) {
      markup += "<img src='/nuxeo/icons/" + entry.type + ".png'/>";
    }
    markup += entry.label;
    if (entry.warn_message) {
      markup += "<img src='/nuxeo/icons/warning.gif' title='"
          + entry.warn_message + "'/>"
    }
    return markup;
  }

    function docEntryDefaultFormatter(doc) {
    var markup = "";
    if (doc.properties['common:icon']) {
      markup += "<img src='/nuxeo" + doc.properties['common:icon'] + "'/>"
    }
    markup += doc.title;
    if (doc.warn_message) {
      markup += "<img src='/nuxeo/icons/warning.gif' title='"
          + doc.warn_message + "'/>"
    }
    return markup;
  }

    function dirEntryDefaultFormatter(entry) {
    var markup = entry.label;
    if (entry.warn_message) {
      markup += "<img src='/nuxeo/icons/warning.gif' title='"
          + entry.warn_message + "'/>"
    }
    return markup;
  }

  var userSelecionDefaultFormatter = userEntryDefaultFormatter;

  function docSelectionDefaultFormatter(doc) {
    var markup = "";
    if (doc.properties['common:icon']) {
      markup += "<img src='/nuxeo" + doc.properties['common:icon'] + "'/>"
    }
    markup += getDocLinkElt(doc);
    if (doc.warn_message) {
      markup += "<img src='/nuxeo/icons/warning.gif' title='"
          + doc.warn_message + "'/>"
    }
    return markup;
  }

  var dirSelectionDefaultFormatter = dirEntryDefaultFormatter;

  function getOperationName(params) {
    var opName = params.operationId;
    if (typeof (opName) == 'undefined' || opName == '') {
      if (params.directoryName && params.directoryName.length > 0) {
        opName = 'Directory.SuggestEntries';
      } else {
        opName = 'Document.PageProvider';
      }
    }
    return opName;
  }

  function getDocLinkElt(doc) {
    var url = doc.contextParameters.documentURL;
    var markup = "<a href=" + url + " onclick='if(!(event.ctrlKey||event.metaKey||event.button==1)){this.href='" + getUrlWithConversationId(url) + "'}'>" + doc.title + "</a>"
    return markup;
  }

  function getUrlWithConversationId(url) {
    return url + "?conversationId=" + currentConversationId;
  }

  function configureOperationParameters(op, params, query) {
    if (params.directoryName && params.directoryName.length > 0) {
      // build default operation for Directory
      op.addParameter("directoryName", params.directoryName);
      op.addParameter("prefix", query.term);
      op.addParameter("translateLabels", params.translateLabels);
      op.addParameter("lang", currentUserLang);
      op.addParameter("labelFieldName", params.labelFieldName);
      op.addParameter("dbl10n", params.dbl10n);
      op.addParameter("filterParent", params.filterParent);
      op.addParameter("canSelectParent", params.canSelectParent);
      op.addParameter("separator", params.separator);
      op.addParameter("obsolete", params.obsolete);
    } else if (params.operationId == 'UserGroup.Suggestion') {
      op.addParameter("prefix", query.term);
      op.addParameter("searchType", params.searchType);
    } else {
      // build default operation for Document
      op.addParameter("queryParams", query.term + "%");
      op.addParameter("query", params.query);
      op.addParameter("providerName", params.pageProviderName);
      op.addParameter("page", "0");
      op.addParameter("pageSize", "20");
    }
  }

  function fillResult(results, data, params) {
    if ((params.directoryName && params.directoryName.length > 0)
        || params.operationId == 'UserGroup.Suggestion') {
      // default result parsing for Directory entries
      for (i = 0; i < data.length; i++) {
        var entry = data[i];
        results.push(entry);
      }
    } else {
      // default result parsing for Documents
      for (i = 0; i < data.entries.length; i++) {
        var doc = data.entries[i];
        results.push(doc);
      }
    }
  }

  function getDefaultLabel(item) {
    if (item.label) {
      return item.label;
    } else {
      return item.title;
    }
  }

  function getDefaultId(item) {
    if (item.computedId) {
      return item.computedId;
    } else if (item.uid) {
      return item.uid;
    } else {
      return item.id;
    }
  }

  function initSelect2Widget(el) {
    // retrieve parameters from Html
    var elid = el.attr("id");
    var params = {};

    var paramId = (elid + "_params").split(":").join("\\:");
    var paramsHolder = jQuery("#" + paramId);
    params = JSON.parse(paramsHolder.val());

    var readonly = params.readonly == 'true';
    var required = params.required == 'true';

    var initId = (elid + "_init").split(":").join("\\:");
    var initHolder = jQuery("#" + initId);
    var initDoc = null;
    try {
      initDoc = JSON.parse(initHolder.val());
    } catch (err) {
      console.log("Unable to parse initvalue", err, initHolder.val())
    }

    // set style on select
    el.css("width", params.width + "px");

    // determine operation name
    var opName = getOperationName(params);

    // define automation params
    var automationParams = {
      "documentSchemas" : params.documentSchemas
    };

    if (params.repository) {
      automationParams.repository = params.repository;
    }

    // init Automation Operation
    var op = jQuery().automation(opName, automationParams);

    // detect if we need custom selection formatting
    var selectionFormatterFunction = null;
    if (params.selectionFormatter && params.selectionFormatter.length > 0) {
      selectionFormatterFunction = eval(params.selectionFormatter);
    }
    // detect if we need custom suggestion formatting
    var suggestionFormatterFunction = null;
    if (params.suggestionFormatter && params.suggestionFormatter.length > 0) {
      suggestionFormatterFunction = eval(params.suggestionFormatter);
    }

    // build select2 parameters
    var select2_params = {
      containerCssClass : params.containerCssClass,
      dropdownCssClass : params.dropdownCssClass,
      minimumInputLength : params.minChars,
      query : function(query) {

        configureOperationParameters(op, params, query);

        op.execute(function(data, textStatus, xhr) {
          var results = [];
          fillResult(results, data, params)
          query.callback({
            results : results
          });
        });
      }
    }

    // append custom selection formatter if needed
    if (selectionFormatterFunction != null) {
      select2_params.formatSelection = selectionFormatterFunction;
    } else {
      select2_params.formatSelection = function(doc) {
        if (select2_params.labelFieldName != null) {
          return doc.properties[select2_params.labelFieldName];
        } else {
          return getDefaultLabel(doc)
        }
      };
    }
    // append custom suggestion formatter if needed
    if (suggestionFormatterFunction != null) {
      select2_params.formatResult = suggestionFormatterFunction;
    } else {
      select2_params.formatResult = getDefaultLabel;
    }

    // append id formatter if needed
    if (params.idProperty && params.idProperty.length > 0) {
      select2_params.id = function(doc) {
        return doc.properties[params.idProperty];
      };
    } else if (params.prefixed) {
      select2_params.id = function(item) {
        return item.prefixed_id;
      };
    } else {
      select2_params.id = getDefaultId;
    }

    if (initDoc != null) {
      select2_params.initSelection = function(element, callback) {
        callback(initDoc);
      };
    }

    if (params.multiple == 'true') {
      select2_params.maximumSelectionSize = params.maximumSelectionSize;
      select2_params.multiple = true;
    }

    if (params.placeholder && !readonly) {
      select2_params.placeholder = params.placeholder;
      select2_params.allowClear = !required;
    }

    // init select2
    el.select2(select2_params);

    // view or edit mode
    if (readonly) {
      el.select2("readonly", true);
    }

    if (!readonly) {

      // Make selected items sortable
      if (params.multiple == 'true' && params.sortable == 'true') {
        el.select2("container").find("ul.select2-choices").sortable({
          containment : 'parent',
          start : function() {
            el.select2("onSortStart");
          },
          update : function() {
            el.select2("onSortEnd");
          }
        });
      }

      // trigger for safeEdit restore
      el.on("change", function(e) {
        if (e) {
          if (e.added) {
            var newValue;
            if (select2_params.multiple) {
              var newObj = JSON.parse(initHolder.val());
              newObj.push(e.added);
              var newValue = JSON.stringify(newObj);
            } else {
              newValue = JSON.stringify(e.added);
            }
            initHolder.val(newValue);
          }

          if (e.removed) {
            var removedId = select2_params.id(e.removed);
            if (select2_params.multiple) {
              var newValue = '';
              var tempObj = JSON.parse(initHolder.val());
              jQuery.each(tempObj, function(index, result) {
                if (result) {
                  if (select2_params.id(result) == removedId) {
                    tempObj.splice(index, 1);
                  }
                }
              });
              newValue = JSON.stringify(tempObj);
              initHolder.val(newValue);
            } else {
              if (initHolder.val() == removedId) {
                initHolder.val('');
              }
            }
          }
        }

        // ReRender any jsf component if needed.
        if (params.reRenderFunctionName) {
          window[params.reRenderFunctionName]();
        }
      });
    }
  }

  window.initSelect2AjaxWidget = function initSelect2AjaxWidget(widgetId, index) {

    var selector = "input[type='hidden'][id$=";

    if (index != null && index.length > 0) {
      selector = selector + index + "\\:";
    }
    selector = selector + widgetId + "_select2]";

    initSelect2Widget(jQuery(selector));

  }

  window.initSelect2Widgets = function initSelect2Widgets() {
    jQuery("input[type='hidden'][id$=select2]").each(function(idx, el) {
      initSelect2Widget(jQuery(el));
    });
  }

})();