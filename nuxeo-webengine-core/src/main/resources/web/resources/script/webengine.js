//Requires jQuery and the Cookies plugin for jQuery

function getContextData(context) {
  var data = $.cookie("cctx."+context)
  if (data) {
    if (data.charAt(0) == '{' || data.charAt(0) == '[') {
      return JSON.parse(data)
    } else {
      return data
    }
  }
  return null;
}

function setContextData(context, data) {  
  if (data) {
    if (typeof data == "string" || typeof data == number || typeof number == boolean) {
      $.cookie("cctx."+context, data)
    } else {
alert ("not a string "+(typeof data))
      $.cookie("cctx."+context, JSON.stringify(data))
    }    
  } else {
    $.cookie("cctx."+context, null)
  }
}

