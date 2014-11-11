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
    eventPosX: 'pageX',
    eventPosY: 'pageY',
    shadow : true,
    onContextMenu: null,
    onShowMenu: null,
    bind: 'contextmenu',
    useFilter: true,
    anchor: 'body',
    ctxMenuStyle : 'ctxMenuStyle',
    ctxMenuItemHoverStyle : 'ctxMenuItemHoverStyle',
    ctxMenuItemStyle : 'ctxMenuItemStyle',
    ctxMenuImg : 'ctxMenuImg'
   };

  jQuery.fn.contextMenu = function(id, options) {
    if (!options)
       options={};
    hash = hash || [];
    hash.push({
      id : id,
      bindings: options.bindings || null,
      shadow: options.shadow || options.shadow === false ? options.shadow : defaults.shadow,
      onContextMenu: options.onContextMenu || defaults.onContextMenu,
      onShowMenu: options.onShowMenu || defaults.onShowMenu,
      eventPosX: options.eventPosX || defaults.eventPosX,
      eventPosY: options.eventPosY || defaults.eventPosY,
      bind: options.bind || defaults.bind,
      useFilter: options.useFilter || options.useFilter === false ? options.useFilter : defaults.useFilter,
      anchor: options.anchor || defaults.anchor,
      ctxMenuStyle: options.ctxMenuStyle || defaults.ctxMenuStyle,
      ctxMenuItemHoverStyle: options.ctxMenuItemHoverStyle || defaults.ctxMenuItemHoverStyle,
      ctxMenuItemStyle: options.ctxMenuItemStyle || defaults.ctxMenuItemStyle,
      ctxMenuImg: options.ctxMenuImg || defaults.ctxMenuImg
    });
    var index = hash.length - 1;

    if (!menu) {                                      // Create singleton menu
        menu = jQuery('<div id="jqContextMenu"></div>')
                 .hide()
                 .css({position:'absolute', zIndex:'500'})
                 .appendTo(hash[index].anchor)
                 .bind('click', function(e) {
                   e.stopPropagation();
                 });
      }
      if (!shadow) {
        shadow = jQuery('<div></div>')
                   .addClass('ctxMenuShadow')
                   .appendTo(hash[index].anchor)
                   .hide();
      }

    jQuery(this).bind(hash[index].bind, function(e) {
      // Check if onContextMenu() defined
      var bShowContext = (!!hash[index].onContextMenu) ? hash[index].onContextMenu(e) : true;
      if (bShowContext) display(index, this, e, hash[index]);
      return false;
    });
    return this;
  };

  function display(index, trigger, e, options) {
    var cur = hash[index];
    content = jQuery('#'+cur.id).find('ul:first').clone(true);
        content.addClass(options.ctxMenuStyle);
      content.find('li').addClass(options.ctxMenuItemStyle).hover( function() {
        jQuery(this).toggleClass(options.ctxMenuItemHoverStyle);
        jQuery(this).toggleClass(options.ctxMenuItemStyle);
      }, function() {
        jQuery(this).toggleClass(options.ctxMenuItemHoverStyle);
        jQuery(this).toggleClass(options.ctxMenuItemStyle);
      }).find('img').addClass(options.ctxMenuImg);
    content.find('li').bind('click', hide);
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
  beforeDisplayCallBack(e,cur,menu,shadow,trigger,e.pageX,e.pageY, options.useFilter);
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
    if (actionsToRemove) {
    // filter menu items
    var deleteQuery = null;
    for (i = 0; i < actionsToRemove.length; i++) {
      if (!deleteQuery)
        deleteQuery = '#ctxMenu_' + actionsToRemove[i];
      else
        deleteQuery = deleteQuery + ',#ctxMenu_' + actionsToRemove[i];
    }

    if (actionsToRemove.length > 0)
      jQuery(deleteQuery, menu).remove();
  }
    // display menu
    menu.css({'left':menuX,'top':menuY}).show();
    if (cur.shadow) shadow.css({width:menu.width(),height:menu.height(),left:menuX+2,top:menuY+2}).show();
    jQuery(document).one('click', hideMenu);
}


function getDocRef(trigger)
{
  return trigger.getAttribute('docref');
}

function beforeDisplayCallBack(e,cur,menu,shadow,trigger,menuX,menuY,useFilter)
{
    // save call context
    currentMenuContext = {'e':e,'cur':cur,'menu':menu,'shadow':shadow,'menuX':menuX,'menuY':menuY};

    var docRef=getDocRef(trigger);

    if (useFilter) {
    // trigger Seam filter call
    getMenuItemsToHide(docRef);
  } else {
    getMenuItemsToHideCallBacks();
  }
}

function hideMenu()
{
    menu=currentMenuContext['menu'];
    shadow=currentMenuContext['shadow'];
    menu.hide();
    shadow.hide();
}

function setupContextMenu(target, id, options)
{
  var menuId;
  if (id) menuId = id;
  else menuId = "popupMenu";
  if (options) {
    if (options.bind)
      options.onContextMenu = function(e) {
        if (e.type == options.bind)
          return true;
        else
          return false;
      }
  }
  jQuery(document).ready(function(){
    jQuery(target).contextMenu(menuId, options);
  });
}