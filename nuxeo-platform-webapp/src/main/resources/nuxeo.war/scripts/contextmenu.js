/*
 * Modified for Nuxeo EP 5 integration
 *
 * ContextMenu - jQuery plugin for right-click context menus
 *
 * Author: Chris Domigan
 * Contributors: Dan G. Switzer, II
 * Parts of this plugin are inspired by Joern Zaefferer's Tooltip plugin
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 *
 * Version: r2
 * Date: 16 July 2007
 *
 * For documentation visit http://www.trendskitchens.co.nz/jquery/contextmenu/
 *
 */
jQuery.noConflict();

(function(jQuery) {

 	var menu, shadow, trigger, content, hash, currentTarget;
  var defaults = {
    menuStyle: {
      listStyle: 'none',
      padding: '1px',
      margin: '0px',
      backgroundColor: '#fff',
      border: '1px solid #999',
      width: '100px'
    },
    itemStyle: {
      margin: '0px',
      color: '#000',
      display: 'block',
      cursor: 'default',
      padding: '3px',
      border: '1px solid #fff',
      backgroundColor: 'transparent',
      fontSize: '9pt'
    },
    itemHoverStyle: {
      border: '1px solid #0a246a',
      backgroundColor: '#b6bdd2'
    },
    eventPosX: 'pageX',
    eventPosY: 'pageY',
    shadow : true,
    onContextMenu: null,
    onShowMenu: null
 	};

  jQuery.fn.contextMenu = function(id, options) {
    if (!options)
       options={};
    if (!menu) {                                      // Create singleton menu
      menu = jQuery('<div id="jqContextMenu"></div>')
               .hide()
               .css({position:'absolute', zIndex:'500'})
               .appendTo('body')
               .bind('click', function(e) {
                 e.stopPropagation();
               });
    }
    if (!shadow) {
      shadow = jQuery('<div></div>')
                 .css({backgroundColor:'#000',position:'absolute',opacity:0.2,zIndex:499})
                 .appendTo('body')
                 .hide();
    }
    hash = hash || [];
    hash.push({
      id : id,
      menuStyle: jQuery.extend({}, defaults.menuStyle, options.menuStyle || {}),
      itemStyle: jQuery.extend({}, defaults.itemStyle, options.itemStyle || {}),
      itemHoverStyle: jQuery.extend({}, defaults.itemHoverStyle, options.itemHoverStyle || {}),
      bindings: options.bindings || null,
      shadow: options.shadow || options.shadow === false ? options.shadow : defaults.shadow,
      onContextMenu: options.onContextMenu || defaults.onContextMenu,
      onShowMenu: options.onShowMenu || defaults.onShowMenu,
      eventPosX: options.eventPosX || defaults.eventPosX,
      eventPosY: options.eventPosY || defaults.eventPosY
    });

    var index = hash.length - 1;
    jQuery(this).bind('contextmenu', function(e) {
      // Check if onContextMenu() defined
      var bShowContext = (!!hash[index].onContextMenu) ? hash[index].onContextMenu(e) : true;
      if (bShowContext) display(index, this, e, options);
      return false;
    });
    return this;
  };

  function display(index, trigger, e, options) {
    var cur = hash[index];
    content = jQuery('#'+cur.id).find('ul:first').clone(true);
    content.css(cur.menuStyle).find('li').css(cur.itemStyle).hover(
      function() {
        jQuery(this).css(cur.itemHoverStyle);
      },
      function(){
        jQuery(this).css(cur.itemStyle);
      }
    ).find('img').css({verticalAlign:'middle',paddingRight:'2px'});

    // Send the content to the menu
    menu.html(content);

    // if there's an onShowMenu, run it now -- must run after content has been added
		// if you try to alter the content variable before the menu.html(), IE6 has issues
		// updating the content
    if (!!cur.onShowMenu) menu = cur.onShowMenu(e, menu);

    // introspec binding from html menu
    if (!cur.bindings)
    {
       cur.bindings={};
       menuHtml=document.getElementById(cur.id);
       els=menuHtml.getElementsByTagName("li");
       for(i=0;i<els.length;i++)
       {
          fct = els[i].getAttribute('action');
          if (fct)
          {
             cur.bindings[els[i].id]=eval(fct);
          }
       }
    }

    jQuery.each(cur.bindings, function(id, func) {
      jQuery('#'+id, menu).bind('click', function(e) {
        hide();
        func(getDocRef(trigger), currentTarget, trigger);
      });
    });

    jQuery(document).one('click', hide);
    beforeDisplayCallBack(e,cur,menu,shadow,trigger,e.pageX,e.pageY);
  }

  function show() {
    menu.show();
    shadow.show();
  }

  function hide() {
    menu.hide();
    shadow.hide();
  }

  // Apply defaults
  jQuery.contextMenu = {
    defaults : function(userDefaults) {
      jQuery.each(userDefaults, function(i, val) {
        if (typeof val == 'object' && defaults[i]) {
          jQuery.extend(defaults[i], val);
        }
        else defaults[i] = val;
      });
    }
  };

})(jQuery);

jQuery(function() {
  jQuery('div.contextMenu').hide();
});



// Nuxeo integration
var currentMenuContext = {};

// Seam remoting call
function getMenuItemsToHide(docRef)
{
    Seam.Component.getInstance("popupHelper").getUnavailableActionId(docRef,getMenuItemsToHideCallBacks);
}

// Seam remoting callback
function getMenuItemsToHideCallBacks(actionsToRemove)
{
    // restore context
    menu=currentMenuContext['menu'];
    shadow=currentMenuContext['shadow'];
    e=currentMenuContext['e'];
    cur=currentMenuContext['cur'];
    menuX=currentMenuContext['menuX'];
    menuY=currentMenuContext['menuY'];

    // filter menu items
    var deleteQuery=null;
    for(i=0;i<actionsToRemove.length;i++)
    {
      if (!deleteQuery)
       deleteQuery='#ctxMenu_' + actionsToRemove[i];
      else
       deleteQuery=deleteQuery + ',#ctxMenu_' + actionsToRemove[i];
    }

     if (actionsToRemove.length>0)
     	jQuery(deleteQuery, menu).remove();

    // display menu
    menu.css({'left':menuX,'top':menuY}).show();
    if (cur.shadow) shadow.css({width:menu.width(),height:menu.height(),left:menuX+2,top:menuY+2}).show();
    jQuery(document).one('click', hideMenu);
}


function getDocRef(trigger)
{
  return trigger.getAttribute('docRef');
}

function beforeDisplayCallBack(e,cur,menu,shadow,trigger,menuX,menuY)
{
    // save call context
    currentMenuContext = {'e':e,'cur':cur,'menu':menu,'shadow':shadow,'menuX':menuX,'menuY':menuY};

    var docRef=getDocRef(trigger);
    // trigger Seam filter call
    getMenuItemsToHide(docRef);
}

function hideMenu()
{
    menu=currentMenuContext['menu'];
    shadow=currentMenuContext['shadow'];
    menu.hide();
    shadow.hide();
}


function setupContextMenu(target)
{
  if (window.addEventListener) {
    window.addEventListener("load", function (e) {jQuery(target).contextMenu("popupMenu")}, true);
  } else if (window.attachEvent) {
	window.attachEvent("onload", function (e) {jQuery(target).contextMenu("popupMenu")});
  }
}