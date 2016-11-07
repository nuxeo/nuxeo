//Requires jQuery and the Cookies plugin for jQuery

function getContextData(context) {
  var data = $.cookie("vars."+context);
  if (data) {
    if (data.charAt(0) == '{' || data.charAt(0) == '[') {
      return JSON.parse(data);
    } else {
      return data;
    }
  }
  return null;
}

function setContextData(context, data) {
  if (data) {
    if (typeof data == "string" || typeof data == number || typeof number == boolean) {
      $.cookie("vars."+context, data);
    } else {
      alert ("not a string "+(typeof data));
      $.cookie("vars."+context, JSON.stringify(data));
    }
  } else {
    $.cookie("vars."+context, null);
  }
}
