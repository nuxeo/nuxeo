/**
 *  author:		Timothy Groves - http://www.brandspankingnew.net
 *	version:	1.2 - 2006-11-17
 *              1.3 - 2006-12-04
 *              2.0 - 2007-02-07
 *              2.1.1 - 2007-04-13
 *              2.1.2 - 2007-07-07
 *              2.1.3 - 2007-07-19
 *
 */


if (typeof(bsn) == "undefined")
	_b = bsn = {};


if (typeof(_b.Autosuggest) == "undefined")
	_b.Autosuggest = {};
else
	alert("Autosuggest is already set!");



_b.AutoSuggest = function (id, param)
{
	// no DOM - give up!
	//
	if (!document.getElementById)
		return 0;




	// get field via DOM
	//
	this.fld = _b.DOM.gE(id);

	if (!this.fld)
		return 0;

	this.hiddenfld = _b.DOM.gE('hidden'+id);

	if (!this.hiddenfld)
		return 0;



	// init variables
	//
	this.sInp 	= "";
	this.nInpC 	= 0;
	this.aSug 	= [];
	this.iHigh 	= 0;

	// parameters object
	//
	this.oP = param ? param : {};

	// defaults
	//
	var k, def = {minchars:1, varname:"input", className:"autosuggest", timeout:2500, delay:500, offsety:-5, shownoresults: true, noresults: "No results!", cache: true, maxentries: 25, directoryName: "", displayIdAndLabel: false, cacheDirectory: true, directoryValues: ""};
	for (k in def)
	{
		if (typeof(this.oP[k]) != typeof(def[k]))
			this.oP[k] = def[k];
	}
	var p = this;
	//init local Cache
	if (this.oP.cacheDirectory){
		var initDirectory = function(result){
			p.directoryValues = [];
			var jsondata = eval('(' + result + ')');
			for (var i=0;i<jsondata.results.length;i++){
				p.directoryValues.push(  { 'id':jsondata.results[i].id, 'value':jsondata.results[i].value, 'info':jsondata.results[i].info }  );
			}
		}
		if (!this.oP.directoryValues) {Seam.Component.getInstance("suggestBox").getSuggestedValues(this.oP.directoryName, "", initDirectory);}
		else {initDirectory(this.oP.directoryValues.substring(1,this.oP.directoryValues.length - 1));}
	}
	// set keyup handler for field
	// and prevent autocomplete from client
	// NOTE: not using addEventListener because UpArrow fired twice in Safari
	//_b.DOM.addEvent( this.fld, 'keyup', function(ev){ return pointer.onKeyPress(ev); } );

	this.fld.onkeypress 	= function(ev){ return p.onKeyPress(ev); };
	this.fld.onkeyup 		= function(ev){ return p.onKeyUp(ev); };
	this.fld.onblur			= function(ev){ return p.onBlur(ev);};
	this.fld.onclick		= function(ev){ return p.onClick(ev);};

	this.fld.setAttribute("autocomplete","off");
};



_b.AutoSuggest.prototype.onClick = function(ev)
{
	if (!this.fld.value){
		var pointer = this;
		var input = this.sInp;
		clearTimeout(this.ajID);
		this.ajID = setTimeout( function() { pointer.doAjaxRequest(input) }, this.oP.delay );
	}
}





_b.AutoSuggest.prototype.onBlur = function(ev)
{
	var bubble = 1;
	this.setHighlightedValue();
	return bubble;
}



_b.AutoSuggest.prototype.onKeyPress = function(ev)
{

	var key = (window.event) ? window.event.keyCode : ev.keyCode;



	// set responses to keydown events in the field
	// this allows the user to use the arrow keys to scroll through the results
	// ESCAPE clears the list
	// TAB sets the current highlighted value
	//
	var RETURN = 13;
	var TAB = 9;
	var ESC = 27;
//	var SPACE = 32;

	var bubble = 1;

	switch(key)
	{
		case TAB:
			this.setHighlightedValue();
			bubble = 0;
			break;

		case RETURN:
			this.setHighlightedValue();
			bubble = 0;
			break;

		case ESC:

 			this.sInp = this.hiddenfld.value = this.fld.value = "";
			this.clearSuggestions();
			bubble = 0;
			break;
	}

	return bubble;
};



_b.AutoSuggest.prototype.onKeyUp = function(ev)
{
	var key = (window.event) ? window.event.keyCode : ev.keyCode;


	var pointer = this;

	// set responses to keydown events in the field
	// this allows the user to use the arrow keys to scroll through the results
	// ESCAPE clears the list
	// TAB sets the current highlighted value
	//

	var ARRUP = 38;
	var ARRDN = 40;
	var DELETE = 46;
	var BACKSPACE = 8;

	var bubble = 1;

	switch(key)
	{

		case ARRUP:
			pointer.changeHighlight(key);
			bubble = 0;
			break;


		case ARRDN:
			pointer.changeHighlight(key);
			bubble = 0;
			break;


		default:
			pointer.getSuggestions(this.fld.value);
	}

	return bubble;


};








_b.AutoSuggest.prototype.getSuggestions = function (val)
{

	// if input stays the same, do nothing
	//
	if ((val == this.sInp) && (val != 0))
		return 0;


	// kill list
	//
	_b.DOM.remE(this.idAs);


	this.sInp = val;


	// input length is less than the min required to trigger a request
	// do nothing
	//
	if ( (val.length < this.oP.minchars))
	{
		this.aSug = [];
		this.nInpC = val.length;
		return 0;
	}




	var ol = this.nInpC; // old length
	this.nInpC = val.length ? val.length : 0;



	// if caching enabled, and user is typing (ie. length of input is increasing)
	// filter results out of aSuggestions from last request
	//
	var l = this.aSug.length;
	if (this.nInpC > ol && l && l<this.oP.maxentries && this.oP.cache)
	{
		var arr = [];
		for (var i=0;i<l;i++)
		{
			if (this.aSug[i].value.substr(0,val.length).toLowerCase() == val.toLowerCase())
				arr.push( this.aSug[i] );
		}
		this.aSug = arr;

		this.createList(this.aSug);



		return false;
	}
	else
	// do new request
	//
	{
		var pointer = this;
		var input = this.sInp;
		clearTimeout(this.ajID);
		this.ajID = setTimeout( function() { pointer.doAjaxRequest(input) }, this.oP.delay );
	}

	return false;
};






_b.AutoSuggest.prototype.doAjaxRequest = function (input)
{

	// check that saved input is still the value of the field
	//
	if (input != this.fld.value)
		return false;

	var pointer = this;


	var input = this.sInp;
	var directoryName = this.oP.directoryName;

	var completionCallback = function(result){
		pointer.setSuggestions(input,result) ;
	};

	if (!this.oP.cacheDirectory){
		Seam.Component.getInstance("suggestBox").getSuggestedValues(directoryName, input, completionCallback);
	}else{
		 pointer.setSuggestions(input,result);
	}
};



_b.AutoSuggest.prototype.setSuggestions = function (input, result)
{

	// if field input no longer matches what was passed to the request
	// don't show the suggestions
	//
	if (input != this.fld.value)
		return false;

	this.aSug = [];
	if (!this.oP.cacheDirectory){
		var jsondata = eval('(' + result + ')');
		for (var i=0;i<jsondata.results.length;i++){
			this.aSug.push(  { 'id':jsondata.results[i].id, 'value':jsondata.results[i].value, 'info':jsondata.results[i].info }  );
		}
	}else{
		var startsWith = function(string, input){
			var taille = input.length;
			var subSection = string.substring(taille,0);
			if (subSection == input){
				return true;
			}
			else{
				return false;
			}
		};
		for (var i=0;i<this.directoryValues.length;i++){
			var label = this.directoryValues[i].value;
			if (label == ""){
				label = this.directoryValues[i].info;
			}
			if (startsWith(label.toLowerCase(),input.toLowerCase())) {
				this.aSug.push(  { 'id':this.directoryValues[i].id, 'value':this.directoryValues[i].value, 'info':this.directoryValues[i].info }  );
			}
		}
	}
	this.idAs = "as_"+this.fld.id;

	this.createList(this.aSug);

};














_b.AutoSuggest.prototype.createList = function(arr)
{
	var pointer = this;




	// get rid of old list
	// and clear the list removal timeout
	//
	_b.DOM.remE(this.idAs);
	this.killTimeout();


	// if no results, and shownoresults is false, do nothing
	//
	if (arr.length == 0 && !this.oP.shownoresults)
		return false;


	// create holding div
	//
	var div = _b.DOM.cE("div", {id:this.idAs, className:this.oP.className});

	var hcorner = _b.DOM.cE("div", {className:"as_corner"});
	var hbar = _b.DOM.cE("div", {className:"as_bar"});
	var header = _b.DOM.cE("div", {className:"as_header"});
	header.appendChild(hcorner);
	header.appendChild(hbar);
	div.appendChild(header);




	// create and populate ul
	//
	var ul = _b.DOM.cE("ul", {id:"as_ul"});




	// loop throught arr of suggestions
	// creating an LI element for each suggestion
	//
	for (var i=0;i<arr.length;i++)
	{
		// format output with the input enclosed in a EM element
		// (as HTML, not DOM)
		//
		var val = arr[i].value;
		if (!val || val == "" ) val = arr[i].info;
		var st = val.toLowerCase().indexOf( this.sInp.toLowerCase() );
		var output = val.substring(0,st) + "<em>" + val.substring(st, st+this.sInp.length) + "</em>" + val.substring(st+this.sInp.length);


		var span 		= _b.DOM.cE("span", {}, output, true);
		if ((arr[i].info != "") && (this.oP.displayIdAndLabel))
		{
			var br			= _b.DOM.cE("br", {});
			span.appendChild(br);
			var small		= _b.DOM.cE("small", {}, arr[i].info);
			span.appendChild(small);
		}

		var a 			= _b.DOM.cE("a", { href:"#" });

		var tl 		= _b.DOM.cE("span", {className:"tl"}, " ");
		var tr 		= _b.DOM.cE("span", {className:"tr"}, " ");
		a.appendChild(tl);
		a.appendChild(tr);

		a.appendChild(span);

		a.name = i+1;
		a.onclick = function () { pointer.setHighlightedValue(); return false; };
		a.onmouseover = function () { pointer.setHighlight(this.name); };

		var li = _b.DOM.cE(  "li", {}, a  );

		ul.appendChild( li );
	}


	// no results
	//
	if (arr.length == 0 && this.oP.shownoresults)
	{
		var li = _b.DOM.cE(  "li", {className:"as_warning"}, this.oP.noresults  );
		ul.appendChild( li );
	}


	div.appendChild( ul );


	var fcorner = _b.DOM.cE("div", {className:"as_corner"});
	var fbar = _b.DOM.cE("div", {className:"as_bar"});
	var footer = _b.DOM.cE("div", {className:"as_footer"});
	footer.appendChild(fcorner);
	footer.appendChild(fbar);
	div.appendChild(footer);



	// get position of target textfield
	// position holding div below it
	// set width of holding div to width of field
	//
	var pos = _b.DOM.getPos(this.fld);
	var fieldHeight = 25;
	if (this.oP.displayIdAndLabel) fieldHeight = 40;
	div.style.left 		= pos.x + "px";
	div.style.top 		= ( pos.y + this.fld.offsetHeight + this.oP.offsety ) + "px";
	div.style.width 	= this.fld.offsetWidth + "px";
	if (arr.length < 6){
		if  (arr.length == 0) {
			div.style.height=fieldHeight + "px";
		}
		else {
			div.style.height=fieldHeight*arr.length + "px";
		}
	}
	else{
		div.style.height=150 + "px";
	}


	// set mouseover functions for div
	// when mouse pointer leaves div, set a timeout to remove the list after an interval
	// when mouse enters div, kill the timeout so the list won't be removed
	//
	//div.onmouseover 	= function(){ pointer.killTimeout() };
	//div.onmouseout 		= function(){ pointer.resetTimeout() };


	// add DIV to document
	//
	document.getElementsByTagName("body")[0].appendChild(div);



	// currently no item is highlighted
	//
	this.iHigh = 0;






	// remove list after an interval
	//
	var pointer = this;
	this.toID = setTimeout(function () { pointer.clearSuggestions() }, this.oP.timeout);
};














_b.AutoSuggest.prototype.changeHighlight = function(key)
{
	var list = _b.DOM.gE("as_ul");
	if (!list)
		return false;

	var n;

	if (key == 40)
		n = this.iHigh + 1;
	else if (key == 38)
		n = this.iHigh - 1;


	if (n > list.childNodes.length)
		n = 1;
	if (n < 1)
		n = list.childNodes.length;



	this.setHighlight(n);
};



_b.AutoSuggest.prototype.setHighlight = function(n)
{
	var list = _b.DOM.gE("as_ul");
	if (!list)
		return false;

	if (this.iHigh > 0)
		this.clearHighlight();

	this.iHigh = Number(n);

	list.childNodes[this.iHigh-1].className = "as_highlight";


	//this.killTimeout();
};


_b.AutoSuggest.prototype.clearHighlight = function()
{
	var list = _b.DOM.gE("as_ul");
	if (!list)
		return false;

	if (this.iHigh > 0)
	{
		list.childNodes[this.iHigh-1].className = "";
		this.iHigh = 0;
	}
};


_b.AutoSuggest.prototype.setHighlightedValue = function ()
{

	if (this.iHigh != 0 && this.aSug[ this.iHigh-1 ])
	{
		this.sInp = this.fld.value = this.aSug[ this.iHigh-1 ].value;
		this.hiddenfld.value = this.aSug[ this.iHigh-1 ].info;
		// move cursor to end of input (safari)
		//
		this.fld.focus();
		if (this.fld.selectionStart)
			this.fld.setSelectionRange(this.sInp.length, this.sInp.length);


		this.clearSuggestions();

		// pass selected object to callback function, if exists
		//
		if (typeof(this.oP.callback) == "function")
			this.oP.callback( this.aSug[this.iHigh-1] );
	}else {
		this.clearSuggestions();
		this.sInp = this.fld.value = "" ;
	}

};













_b.AutoSuggest.prototype.killTimeout = function()
{
	clearTimeout(this.toID);
};

_b.AutoSuggest.prototype.resetTimeout = function()
{
	clearTimeout(this.toID);
	var pointer = this;
	this.toID = setTimeout(function () { pointer.clearSuggestions() }, 1000);
};







_b.AutoSuggest.prototype.clearSuggestions = function ()
{

	this.killTimeout();

	var ele = _b.DOM.gE(this.idAs);
	var pointer = this;
	if (ele)
	{
		_b.DOM.remE(pointer.idAs);
	}
};











// DOM PROTOTYPE _____________________________________________


if (typeof(_b.DOM) == "undefined")
	_b.DOM = {};



/* create element */
_b.DOM.cE = function ( type, attr, cont, html )
{
	var ne = document.createElement( type );
	if (!ne)
		return 0;

	for (var a in attr)
		ne[a] = attr[a];

	var t = typeof(cont);

	if (t == "string" && !html)
		ne.appendChild( document.createTextNode(cont) );
	else if (t == "string" && html)
		ne.innerHTML = cont;
	else if (t == "object")
		ne.appendChild( cont );

	return ne;
};



/* get element */
_b.DOM.gE = function ( e )
{
	var t=typeof(e);
	if (t == "undefined")
		return 0;
	else if (t == "string")
	{
		var re = document.getElementById( e );
		if (!re)
			return 0;
		else if (typeof(re.appendChild) != "undefined" )
			return re;
		else
			return 0;
	}
	else if (typeof(e.appendChild) != "undefined")
		return e;
	else
		return 0;
};



/* remove element */
_b.DOM.remE = function ( ele )
{
	var e = this.gE(ele);

	if (!e)
		return 0;
	else if (e.parentNode.removeChild(e))
		return true;
	else
		return 0;
};



/* get position */
_b.DOM.getPos = function ( e )
{
	var e = this.gE(e);

	var obj = e;

	var curleft = 0;
	if (obj.offsetParent)
	{
		while (obj.offsetParent)
		{
			curleft += obj.offsetLeft;
			obj = obj.offsetParent;
		}
	}
	else if (obj.x)
		curleft += obj.x;

	var obj = e;

	var curtop = 0;
	if (obj.offsetParent)
	{
		while (obj.offsetParent)
		{
			curtop += obj.offsetTop;
			obj = obj.offsetParent;
		}
	}
	else if (obj.y)
		curtop += obj.y;

	return {x:curleft, y:curtop};
};










// FADER PROTOTYPE _____________________________________________



if (typeof(_b.Fader) == "undefined")
	_b.Fader = {};





_b.Fader = function (ele, from, to, fadetime, callback)
{
	if (!ele)
		return 0;

	this.e = ele;

	this.from = from;
	this.to = to;

	this.cb = callback;

	this.nDur = fadetime;

	this.nInt = 50;
	this.nTime = 0;

	var p = this;
	this.nID = setInterval(function() { p._fade() }, this.nInt);
};




_b.Fader.prototype._fade = function()
{
	this.nTime += this.nInt;

	var ieop = Math.round( this._tween(this.nTime, this.from, this.to, this.nDur) * 100 );
	var op = ieop / 100;

	if (this.e.filters) // internet explorer
	{
		try
		{
			this.e.filters.item("DXImageTransform.Microsoft.Alpha").opacity = ieop;
		} catch (e) {
			// If it is not set initially, the browser will throw an error.  This will set it if it is not set yet.
			this.e.style.filter = 'progid:DXImageTransform.Microsoft.Alpha(opacity='+ieop+')';
		}
	}
	else // other browsers
	{
		this.e.style.opacity = op;
	}


	if (this.nTime == this.nDur)
	{
		clearInterval( this.nID );
		if (this.cb != undefined)
			this.cb();
	}
};



_b.Fader.prototype._tween = function(t,b,c,d)
{
	return b + ( (c-b) * (t/d) );
};
