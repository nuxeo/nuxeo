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
  } else {
    // build default operation for Document
    op.addParameter("queryParams", query.term + "%");
    op.addParameter("query", params.query);
    op.addParameter("page", "0");
    op.addParameter("pageSize", "20");
  }
}

function fillResult(results, data, params) {
  if (params.directoryName && params.directoryName.length > 0) {
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

  // detect if we need custom result formating
  var customFormaterFunction = null;
  if (params.customFormater && params.customFormater.length > 0) {
    customFormaterFunction = eval(params.customFormater);
  }

  // build select2 parameters
  var select2_params = {
    minimumInputLength : params.minimumInputLength,
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

  // append custom result formater if needed
  if (customFormaterFunction != null) {
    select2_params.formatResult = customFormaterFunction;
    select2_params.formatSelection = function(doc) {
      if (select2_params.labelProperty != null) {
        return doc.properties[select2_params.labelProperty];
      } else {
        return getDefaultLabel(doc)
      }
    };
  } else {
    select2_params.formatResult = getDefaultLabel;
    select2_params.formatSelection = function(doc) {
      if (select2_params.labelProperty != null) {
        return doc.properties[select2_params.labelProperty];
      } else {
        return getDefaultLabel(doc)
      }
    };
  }

  // append id formater if needed
  if (params.idProperty && params.idProperty.length > 0) {
    select2_params.id = function(doc) {
      return doc.properties[params.idProperty];
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

  if (params.placeholder) {
    select2_params.placeholder = params.placeholder;
    select2_params.allowClear = true;
  }

  // init select2
  el.select2(select2_params);
}

function initSelect2AjaxWidget(widgetId, index) {

  var selector = "input[type='hidden'][id$=";

  if (index != null && index.length > 0) {
    selector = selector + index + "\\:";
  }
  selector = selector + widgetId + "_select2]";

  initSelect2Widget(jQuery(selector));

}
function initSelect2Widgets() {
  jQuery("input[type='hidden'][id$=select2]").each(function(idx, el) {
    initSelect2Widget(jQuery(el));
  });
};

jQuery(document)
    .ready(
        function() {
          jQuery('head')
              .append(
                  '<link rel="stylesheet" href="/nuxeo/css/select2.css" type="text/css" />');
          initSelect2Widgets();
        });