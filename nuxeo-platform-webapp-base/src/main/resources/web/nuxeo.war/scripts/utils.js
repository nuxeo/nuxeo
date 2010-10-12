function disableCheckBoxesIn(name) {
  var table = document.getElementById(name);
  var listOfInputs = table.getElementsByTagName("input");
  var i;
  for( i = 0; i < listOfInputs.length; i++ ){
    if (listOfInputs[i].type=="checkbox"){
      listOfInputs[i].disabled=true;
    }
  }
}

function enableCheckBoxesIn(name) {
  var table = document.getElementById(name);
  var listOfInputs = table.getElementsByTagName("input");
  var i;
  for( i = 0; i < listOfInputs.length; i++ ){
    if (listOfInputs[i].type=="checkbox"){
      listOfInputs[i].disabled=false;
    }
  }
}

function isOneCheckBoxChecked(name) {
  var table = document.getElementById(name);
  if (table)
  {
    var listOfInputs = table.getElementsByTagName("input");
    var i;
    var se
    for( i = 0; i < listOfInputs.length; i++ ){
      if (listOfInputs[i].type=="checkbox"){
          if (listOfInputs[i].checked)
            return true;
      }
    }
    return false;
  }
  else
    return false;
}


function onSelectAllCheckboxClick(tableName, checked) {
  var table = document.getElementById(tableName);
  var listOfInputs = table.getElementsByTagName("input");
  var i;
  for( i = 0; i < listOfInputs.length; i++ ){
    if (listOfInputs[i].type=="checkbox"){
      listOfInputs[i].disabled=true;
      listOfInputs[i].checked=checked;
    }
  }
}


function confirmAction(name) {
  var confirmBegin = "#{messages['label.documents.confirmActionBegin']} ";
  var confirmEnd = "#{messages['label.documents.confirmActionEnd']}";
  var finalStringConfirm = confirmBegin + name + confirmEnd;

  return confirm(finalStringConfirm);
}

function blankSuggestionInput(parent) {
  var children;
  if (typeof parent.children == 'undefined') {
    // mozilla
    children = parent.childNodes;
  } else {
    // IE
    children = parent.children;
  }
  children[0].value='';
}

function trapEnter(evt, submitButtonId) {
  var keycode;
  if (evt)
      ;
  else if (window.event)
      evt = window.event;
  else if (event)
      evt = event;
  else
      return true;

  if (evt.charCode)
      keycode = evt.charCode;
  else if (evt.keyCode)
      keycode = evt.keyCode;
  else if (evt.which)
      keycode = evt.which;
  else
      keycode = 0;

  if (keycode == 13) {     
      var button = document.getElementById(submitButtonId);
      if (button != null) {
        button.select();
      }
      return false;
  } else {
      return true;
  }
}

