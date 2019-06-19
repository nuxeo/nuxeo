//Copyright (c) 2013 Crystalline Technologies
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the 'Software'),
//  to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
//  and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
//  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(function(jQuery){

  //Main method
  jQuery.fn.json2html = function(json, transform, _options){

    //Make sure we have the json2html base loaded
    if(typeof json2html === 'undefined') return(undefined);

    //Default Options
    var options = {
      'append':true,
      'replace':false,
      'prepend':false,
      'eventData':{}
    };

    //Extend the options (with defaults)
    if( _options !== undefined ) jQuery.extend(options, _options);

    //Insure that we have the events turned (Required)
    options.events = true;

    //Make sure to take care of any chaining
    return this.each(function(){

      //let json2html core do it's magic
      var result = json2html.transform(json, transform, options);

      //Attach the html(string) result to the DOM
      var dom = jQuery(document.createElement('i')).html(result.html);

      //Determine if we have events
      for(var i = 0; i < result.events.length; i++) {

        var event = result.events[i];

        //find the associated DOM object with this event
        var obj = jQuery(dom).find("[json2html-event-id-"+event.type+"='" + event.id + "']");

        //Check to see if we found this element or not
        if(obj.length === 0) throw 'jquery.json2html was unable to attach event ' + event.id + ' to DOM';

        //remove the attribute
        jQuery(obj).removeAttr('json2html-event-id-'+event.type);

        //attach the event
        jQuery(obj).on(event.type,event.data,function(e){
          //attach the jquery event
          e.data.event = e;

          //call the appropriate method
          e.data.action.call(jQuery(this),e.data);
        });
      }

      //Append it to the appropriate element
      if (options.replace) jQuery.fn.replaceWith.call(jQuery(this),jQuery(dom).children());
      else if (options.prepend) jQuery.fn.prepend.call(jQuery(this),jQuery(dom).children());
      else jQuery.fn.append.call(jQuery(this),jQuery(dom).children());
    });
  };
})(jQuery);
