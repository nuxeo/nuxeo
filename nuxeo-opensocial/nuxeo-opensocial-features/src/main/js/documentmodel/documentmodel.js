var documentloader = documentloader
    || {
      load : function(path, callback, instance) {
        var datetmp = new Date();
        var url = path + "/@json/gadget?junk=" + datetmp.getTime();
        var parameters = [];
        var headers = [];
        var me = this;

        var now = new Date().toUTCString();
        headers["Date", now];

        headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
        headers["Pragma", "no-cache"];
        headers["Cache-control"] = "no-cache, must-revalidate";
        headers["X-NUXEO-INTEGRATED-AUTH"] = readCookie("JSESSIONID");

        parameters[gadgets.io.RequestParameters.HEADERS] = headers;
        parameters[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.JSON;
        parameters[gadgets.io.RequestParameters.METHOD] = gadgets.io.MethodType.GET;

        gadgets.io.makeRequest(url, function(response) {
          callback(response, instance);
        }, parameters);
      }
    };

var documentmodel = documentmodel || {
  CHILD_KEY :"child-"
};

documentmodel.documentModel = function(json) {
  this.model = eval('(' + json + ')');
}

documentmodel.documentModel.prototype.getChild = function(index) {
  return this.model.childs[ [ documentmodel.CHILD_KEY, index ].join("")];
};

documentmodel.documentModel.prototype.getChilds = function() {
  return this.model.childs;
};

documentmodel.documentModel.prototype.getDocument = function() {
  return this.model;
};

var htmlrender = htmlrender || {};

htmlrender.htmlRender = function(doc, template, target) {
  this.doc = doc;
  this.template = template;
  this.target = target;
};

htmlrender.htmlRender.prototype.loadTemplate = function(afterLoad) {
  var me = this;
  var parameters = [];
  parameters[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.DOM;
  parameters[gadgets.io.RequestParameters.METHOD] = gadgets.io.MethodType.GET;
  parameters[gadgets.io.RequestParameters.AUTHORIZATION] = gadgets.io.AuthorizationType.SIGNED;
  gadgets.io.makeRequest(this.template, function(response) {
    me.append(response, afterLoad);
  }, parameters);
};

htmlrender.htmlRender.prototype.append = function(response, afterAppend) {
  var me = this;
  jQuery(me.target).html(response.text);
  me.buildDocument();// .buildChildren();
  afterAppend();
  me.updatePath();
  return me;
};

htmlrender.htmlRender.prototype.buildDocument = function() {
  this.setDocument(this.doc);
  return this;
};

htmlrender.htmlRender.prototype.updatePath = function() {
  this.setValue(this.doc.path);
};

htmlrender.htmlRender.prototype.buildChildren = function() {
  var me = this;
  jQuery.each(me.doc.childs, function(index, child) {
    me.setDocument(child);
  });
  return this;
};

htmlrender.htmlRender.prototype.setDocument = function(doc) {
  this.setValue(doc.title).setValue(doc.description).setValue(doc.path);
  return this;
};

htmlrender.htmlRender.prototype.setValue = function(obj) {
  var me = this;

  function getSize(elem) {
    var size = "@view/";
    if (jQuery(elem).hasClass("minithumb")) {
      size += "Thumbnail";
    } else if (jQuery(elem).hasClass("thumb")) {
      size += "Thumbnail";
    } else if (jQuery(elem).hasClass("medium")) {
      size += "Medium";
    } else if (jQuery(elem).hasClass("original")) {
      size += "Original";
    }
    return size;
  }
  ;

  jQuery.each(jQuery(me.target).find(obj.classe),
      function(index, elem) {
        if (jQuery(elem).length >= 1) {
          if (elem.tagName == "SPAN"
              || (elem.tagName == "DIV" && jQuery(elem).hasClass(
                  "text"))) {
            jQuery(elem).text(obj.value);
          } else if (elem.tagName == "IMG") {
            jQuery(elem).attr("src",
                [ obj.value, getSize(elem) ].join(""));
          } else if (elem.tagName == "DIV") {
            jQuery(elem).css(
                "background-image",
                [ "url(", obj.value, getSize(elem), ")" ]
                    .join(""));
          } else if (elem.tagName == "A"
              && jQuery(elem).hasClass("minithumb")) {
            jQuery(elem).attr("href", "#");
            jQuery(elem).css(
                "background-image",
                [ "url(", obj.value, getSize(elem), ")" ]
                    .join(""));
          } else if (elem.tagName == "A") {
            jQuery(elem).attr("href" + obj.value);
          }
        }
      });
  return this;
};

function readCookie(name) {
  var nameEQ = name + "=";
  var ca = document.cookie.split(';');
  for ( var i = 0; i < ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ')
      c = c.substring(1, c.length);
    if (c.indexOf(nameEQ) == 0)
      return c.substring(nameEQ.length, c.length);
  }
  return null;
}
