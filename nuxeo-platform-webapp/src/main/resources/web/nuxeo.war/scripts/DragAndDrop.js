function moveElement(element,containerId){
    //Seam.Remoting.contextPath = "/nuxeo";
    Seam.Remoting.getContext().setConversationId(currentConversationId);
    Seam.Component.getInstance("FileManageActions").moveWithId(element.id,containerId,moveCallback);
}

function copyElement(element){
    //Seam.Remoting.contextPath = "/nuxeo";
    Seam.Remoting.getContext().setConversationId(currentConversationId);
    Seam.Component.getInstance("FileManageActions").copyWithId(element.id,copyCallback);
}

function copyElementIfIdNotStartsWith(element, idPrefix){
    //Seam.Remoting.contextPath = "/nuxeo";
    if (element && element.id.indexOf(idPrefix) <= -1) {
        Seam.Remoting.getContext().setConversationId(currentConversationId);
        Seam.Component.getInstance("FileManageActions").copyWithId(element.id,copyCallback);    	
    }
}

function pasteElement(element){
    //Seam.Remoting.contextPath = "/nuxeo";
    Seam.Remoting.getContext().setConversationId(currentConversationId);
    Seam.Component.getInstance("FileManageActions").pasteWithId(element.id,pasteCallback);
}

// CallBack from the Seam AJAX request
var result;
function moveCallback(result) {
    var moveTrans = "MOVE_ERROR";
    doRefreshPage();
    if(typeof(result)!="undefined"){
        if(result.substring(0,moveTrans.length)==moveTrans){
           alert(result.substring(moveTrans.length,result.length));
        }
    } else{
        alert("undefined result in moveCallback")
    }
}

// CallBack from the Seam AJAX request
var result;
function copyCallback(result) {
    var moveTrans = "COPY_ERROR";
    doRefreshPage();
    if(typeof(result)!="undefined"){
        if(result.substring(0,moveTrans.length)==moveTrans){
           alert(result.substring(moveTrans.length,result.length));
        }
    } else{
        alert("undefined result in copyCallback")
    }
}

// CallBack from the Seam AJAX request
var result;
function pasteCallback(result) {
    var moveTrans = "PASTE_ERROR";
    doRefreshPage();
    if(typeof(result)!="undefined"){
        if(result.substring(0,moveTrans.length)==moveTrans){
           alert(result.substring(moveTrans.length,result.length));
        }
    } else{
        alert("undefined result in pasteCallback")
    }
}

// Refresh page and takes care of conversation propagation
function doRefreshPage()
{
    var cUrl=window.location.href;
    if (cUrl.indexOf(currentConversationId)<0)
    {
        if (cUrl.indexOf("?")>0)
            cUrl+="?conversationId=" + currentConversationId;
        else
            cUrl+="&conversationId=" + currentConversationId;
    }
    window.location.href=cUrl;
}