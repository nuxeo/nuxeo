//cpriceputu@nuxeo.com contextKeeper script
Array.prototype.remove = function(from, to) {
	var rest = this.slice((to || from) + 1 || this.length);
	this.length = from < 0 ? this.length + from : from;
	return this.push.apply(this, rest);
};

// checks whether keeper exists
function keeperExists() {
	if (document.fileInputContextKeeper == null)
		return false;
	if (document.fileInputContextKeeper == 'undefined')
		return false;

	return true;
}

// creates one
function createKeeper() {
	document.fileInputContextKeeper = new Array();
}

// Get the keeper or create one if there is none
function getKeeper() {
	if (!keeperExists()) {
		createKeeper();
	}

	return document.fileInputContextKeeper;
}

// Gets an input from keeper
function getInputFromKeeper(index) {

	if (getKeeper().length > index) {
		return getKeeper()[index];
	}

	return null;
}

// Puts an object in the keeper
function putInKeep(obj) {
	getKeeper().push(obj);
}

function clearKeeper() {
	createKeeper();
}

function getIndexFromId(id) {
	var arr = id.split(':');
	var nr = -1;
	for ( var i = 0; i < arr.length; i++) {

		var lnr = parseInt(arr[i]);
		if (!isNaN(lnr)) {
			nr = lnr;
		}
	}

	return nr;
}

function rebuildIdIndex(id, newIndex) {
	var arr = id.split(':');
	var index = -1;
	var ret = "";
	for ( var i = 0; i < arr.length; i++) {

		var lnr = parseInt(arr[i]);
		if (!isNaN(lnr)) {
			index = i;
		}
	}

	if (index == -1)
		return null;

	arr[index] = newIndex;
	for ( var i = 0; i < arr.length - 1; i++) {
		ret = ret + arr[i] + ":";
	}

	ret = ret + arr[arr.length - 1];

	return ret;
}

// Removes an input from keeper
function removeFromKeeper(index) {
	try {
		gather();
		
		if (getKeeper().length > index) {

			getKeeper().remove(index);
			for ( var i = 0; i < getKeeper().length; i++) {
				getKeeper()[i].id = rebuildIdIndex(getKeeper()[i].id, i);
			}
			
			replaceAllInputs();
		}
	} catch (e) {
		alert(e);
	}
}

function replaceAllInputs()
{
	var index = 0;
	// Get all inputs
	var allInputs = document.getElementsByTagName('input');

	// Iterate to see if there is any input file
	for ( var i = 0; i < allInputs.length; i++) {
		if (allInputs[i].type == 'file') {

			var parent = allInputs[i].parentNode;
			var inputInKeeper = getInputFromKeeper(index);

			if (inputInKeeper != null) {
				parent.removeChild(allInputs[i]);
				parent.appendChild(inputInKeeper);
				index++;
			}
		}
	}
}

function gather()
{
	// Get all inputs
	var allInputs = document.getElementsByTagName('input');
	clearKeeper();

	// Iterate to see if there is any input file
	for ( var i = 0; i < allInputs.length; i++) {
		if (allInputs[i].type == 'file') {
			putInKeep(allInputs[i].cloneNode(true));
		}
	}
}

// Used on add button(onclick) to save the file inputs
function onAddFile() {
	try {
		gather();
	} catch (e) {
		alert(e);
	}
}

// User on add button(oncomplete) to restore file input values
function onReturnAnswer() {
	try {
		replaceAllInputs();
	} catch (e) {
		alert(e);
	}
}
