function requestTasks() {
	console.log("hi! i'm a log message!");
	var params = {};
	var headers = {};

	//var cookie = readCookie('JSESSIONID');
	//console.log("we are using cookie:"+cookie);
	
	params[gadgets.io.RequestParameters.CONTENT_TYPE] = gadgets.io.ContentType.DOM;

	var now = new Date().toUTCString();
	headers["Date", now];
	
	headers["Expires", "Fri, 01 Jan 1990 00:00:00 GMT"];
	headers["Pragma", "no-cache"];
	headers["Cache-control"] = "no-cache, must-revalidate";
	

	var encoded = encode64("Administrator:Administrator");
	console.log("encoded in base64 credentials:"+encoded);
	headers["authentication"] = "basic "+encoded;
	
	params[gadgets.io.RequestParameters.HEADERS] = headers
	
	var url = "http://localhost:8080/nuxeo/restAPI/workflowTasks/default";
	gadgets.io.makeRequest(url, response, params);
};
function response(obj) {
	var dom = obj.data;
	var resultHTML = "";
	console.log("starting to parse "+obj);
	var categories = dom.getElementsByTagName("nxt:category");
	var i= 0;
	console.log("number of categories "+categories.length);
	
	while (i < categories.length) {
		categoryNode = categories[i];
		var categoryName = categoryNode.getAttribute("category");
		var tasks = categoryNode.getElementsByTagName("nxt:task")
		console.log("parsing category:"+categoryName+" with "+ tasks.length +" tasks");
		
		resultHTML += openDiv(categoryName);
		
		var j =0 ;
		while (j < tasks.length) {
			var taskNode = tasks[j];
			var value="<nothing>";
			//should we try backup plan?
			if (taskNode.childNodes.length==0) {
				console.log("no node value, using name");
				value = taskNode.getAttribute("name");
			} else {
				value=taskNode.childNodes[0].nodeValue;
			}
			console.log(categoryName + " -> "+value);
			resultHTML += createItem(value);
			++j;
		}
		
		resultHTML += closeDiv(categoryName);
		++i;
	}
	console.log("done parsing!");
	var taskListDiv = _gel("nxTaskList");
	if (taskListDiv==null) {
		console.log("can't find nxTaskList!")
	} else {
		taskListDiv.innerHTML = resultHTML;
	}
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

function openDiv(name) {
	var result =  '<div class="nxcategory-wrapper">';
	result += '<span class="nxcategory-title">'+name+' ';
	result +='<a class="nxcategory-toggle" href="javascript:openCategory('+"'nxcat-"+name+"')"+'">';
	result +='<span class="takeupspace"></span></a>';
	result +="</span>";
	result +='<div class="nxcategory" id="nxcat-'+name+'">';
	return result;
}

function closeDiv(name) {
	return '</div> <!--'+name+' -->';
	return '</div> <!--'+name+'wrapper -->';
}

function openCategory(divname) {
	console.log(divname);
}
function createItem(name) {
	return '<span class="nxcategory-item">'+name+'</span>';
}

gadgets.util.registerOnLoadHandler(requestTasks);
