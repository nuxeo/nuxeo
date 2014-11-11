/*  WysiHat - WYSIWYG JavaScript framework, version 0.1
 *  (c) 2008 Joshua Peek
 *
 *  WysiHat is freely distributable under the terms of an MIT-style license.
 *--------------------------------------------------------------------------*/



var WysiHat = {};

WysiHat.Editor = {
  attach: function(textarea, options) {
    options = $H(options);
    textarea = $(textarea);
    textarea.hide();

    var model = options.get('model') || WysiHat.iFrame;

    return model.create(textarea, function(editArea) {
      var document = editArea.getDocument();
      var window = editArea.getWindow();

      editArea.load();

      Event.observe(window, 'focus', function(event) { editArea.focus(); });
      Event.observe(window, 'blur', function(event) { editArea.blur(); });


      Event.observe(document, 'mouseup', function(event) {
        editArea.fire("wysihat:mouseup");
      });

      Event.observe(document, 'mousemove', function(event) {
        editArea.fire("wysihat:mousemove");
      });

      Event.observe(document, 'keypress', function(event) {
        editArea.fire("wysihat:change");
        editArea.fire("wysihat:keypress");
      });

      Event.observe(document, 'keyup', function(event) {
        editArea.fire("wysihat:change");
        editArea.fire("wysihat:keyup");
      });

      Event.observe(document, 'keydown', function(event) {
        if (event.keyCode == 86)
          editArea.fire("wysihat:paste");
      });

      Event.observe(window, 'paste', function(event) {
        editArea.fire("wysihat:paste");
      });


      editArea.observe("wysihat:change", function(event) {
        event.target.save();
      });

      editArea.focus();
    });
  }
};

WysiHat.Commands = {
  boldSelection: function() {
    this.execCommand('bold', false, null);
  },

  underlineSelection: function() {
    this.execCommand('underline', false, null);
  },

  italicSelection: function() {
    this.execCommand('italic', false, null);
  },

  strikethroughSelection: function() {
    this.execCommand('strikethrough', false, null);
  },

  blockquoteSelection: function() {
    this.execCommand('blockquote', false, null);
  },

  colorSelection: function(color) {
    this.execCommand('forecolor', false, color);
  },

  linkSelection: function(url) {
    this.execCommand('createLink', false, url);
  },

  insertOrderedList: function() {
    this.execCommand('insertorderedlist', false, null);
  },

  insertUnorderedList: function() {
    this.execCommand('insertunorderedlist', false, null);
  },

  insertImage: function(url) {
    this.execCommand('insertImage', false, url);
  },

  insertHTML: function(html) {
    if (Prototype.Browser.IE) {
      var range = this._selection.getRange();
      range.pasteHTML(html);
      range.collapse(false);
      range.select();
    } else {
      this.execCommand('insertHTML', false, html);
    }
  },

  execCommand: function(command, ui, value) {
    var document = this.getDocument();
    document.execCommand(command, ui, value);
  },

  queryStateCommands: $A(['bold', 'italic', 'underline', 'strikethrough']),

  queryCommandState: function(state) {
    var document = this.getDocument();

    if (this.queryStateCommands.include(state))
      return document.queryCommandState(state);
    else if ((f = this['query' + state.capitalize()]))
      return f.bind(this).call();
    else
      return false;
  }
};
WysiHat.Persistence = (function() {
  function outputFilter(text) {
    return text.formatHTMLOutput();
  }

  function inputFilter(text) {
    return text.formatHTMLInput();
  }

  function content() {
    return this.outputFilter(this.rawContent());
  }

  function setContent(text) {
    this.setRawContent(this.inputFilter(text));
  }

  function save() {
    this.textarea.value = this.content();
  }

   function load() {
     this.setContent(this.textarea.value);
  }

  function reload() {
    this.selection.setBookmark();
    this.save();
    this.load();
    this.selection.moveToBookmark();
  }

  return {
    outputFilter: outputFilter,
    inputFilter:  inputFilter,
    content:      content,
    setContent:   setContent,
    save:         save,
    load:         load,
    reload:       reload
  };
})();
WysiHat.Window = (function() {
  function getDocument() {
    return this.contentDocument || this.contentWindow.document;
  }

  function getWindow() {
    if (this.contentDocument)
      return this.contentDocument.defaultView;
    else if (this.contentWindow.document)
      return this.contentWindow;
    else
      return null;
  }

  function focus() {
    this.getWindow().focus();

    if (this.hasFocus)
      return;

    this.hasFocus = true;
  }

  function blur() {
    this.hasFocus = false;
  }

  return {
    getDocument: getDocument,
    getWindow: getWindow,
    focus: focus,
    blur: blur
  };
})();

WysiHat.iFrame = {
  create: function(textarea, callback) {
    var editArea = new Element('iframe', { 'id': textarea.id + '_editor', 'class': 'editor' });

    Object.extend(editArea, WysiHat.Commands);
    Object.extend(editArea, WysiHat.Persistence);
    Object.extend(editArea, WysiHat.Window);
    Object.extend(editArea, WysiHat.iFrame.Methods);

    editArea.attach(textarea, callback);

    textarea.insert({before: editArea});

    return editArea;
  }
};

WysiHat.iFrame.Methods = {
  attach: function(element, callback) {
    this.textarea = element;

    this.observe('load', function() {
      try {
        var document = this.getDocument();
      } catch(e) { return; } // No iframe, just stop

      this.selection = new WysiHat.Selection(this);

      if (this.ready && document.designMode == 'on')
        return;

      this.setStyle({});
      document.designMode = 'on';
      callback(this);
      this.ready = true;
    });
  },

  setStyle: function(styles) {
    var document = this.getDocument();

    var element = this;
    if (!this.ready)
      return setTimeout(function() { element.setStyle(styles); }, 1);

    if (Prototype.Browser.IE) {
      var style = document.createStyleSheet();
      style.addRule("body", "border: 0");
      style.addRule("p", "margin: 0");

      $H(styles).each(function(pair) {
        var value = pair.first().underscore().dasherize() + ": " + pair.last();
        style.addRule("body", value);
      });
    } else if (Prototype.Browser.Opera) {
      var style = Element('style').update("p { margin: 0; }");
      var head = document.getElementsByTagName('head')[0];
      head.appendChild(style);
    } else {
      Element.setStyle(document.body, styles);
    }

    return this;
  },

  rawContent: function() {
    var document = this.getDocument();
    return document.body.innerHTML;
  },

  setRawContent: function(text) {
    var document = this.getDocument();
    if (document.body)
      document.body.innerHTML = text;
  }
};
WysiHat.Editable = {
  create: function(textarea, callback) {
    var editArea = new Element('div', {
      'id': textarea.id + '_editor',
      'class': 'editor',
      'contenteditable': 'true'
    });
    editArea.textarea = textarea;

    Object.extend(editArea, WysiHat.Commands);
    Object.extend(editArea, WysiHat.Persistence);
    Object.extend(editArea, WysiHat.Window);
    Object.extend(editArea, WysiHat.Editable.Methods);

    callback(editArea);

    textarea.insert({before: editArea});

    return editArea;
  }
};

WysiHat.Editable.Methods = {
  getDocument: function() {
    return document;
  },

  getWindow: function() {
    return window;
  },

  rawContent: function() {
    return this.innerHTML;
  },

  setRawContent: function(text) {
    this.innerHTML = text;
  }
};

Object.extend(String.prototype, (function() {
  function formatHTMLOutput() {
    var text = String(this);
    text = text.tidyXHTML();

    if (Prototype.Browser.WebKit) {
      text = text.replace(/(<div>)+/g, "\n");
      text = text.replace(/(<\/div>)+/g, "");

      text = text.replace(/<p>\s*<\/p>/g, "");

      text = text.replace(/<br \/>(\n)*/g, "\n");
    } else if (Prototype.Browser.Gecko) {
      text = text.replace(/<p>/g, "");
      text = text.replace(/<\/p>(\n)?/g, "\n");

      text = text.replace(/<br \/>(\n)*/g, "\n");
    } else if (Prototype.Browser.IE || Prototype.Browser.Opera) {
      text = text.replace(/<p>(&nbsp;|&#160;|\s)<\/p>/g, "<p></p>");

      text = text.replace(/<br \/>/g, "");

      text = text.replace(/<p>/g, '');

      text = text.replace(/&nbsp;/g, '');

      text = text.replace(/<\/p>(\n)?/g, "\n");

      text = text.gsub(/^<p>/, '');
      text = text.gsub(/<\/p>$/, '');
    }

    text = text.gsub(/<b>/, "<strong>");
    text = text.gsub(/<\/b>/, "</strong>");

    text = text.gsub(/<i>/, "<em>");
    text = text.gsub(/<\/i>/, "</em>");

    text = text.replace(/\n\n+/g, "</p>\n\n<p>");

    text = text.gsub(/(([^\n])(\n))(?=([^\n]))/, "#{2}<br />\n");

    text = '<p>' + text + '</p>';

    text = text.replace(/<p>\s*/g, "<p>");
    text = text.replace(/\s*<\/p>/g, "</p>");

    var element = Element("body");
    element.innerHTML = text;

    if (Prototype.Browser.WebKit || Prototype.Browser.Gecko) {
      var replaced;
      do {
        replaced = false;
        element.select('span').each(function(span) {
          if (span.hasClassName('Apple-style-span')) {
            span.removeClassName('Apple-style-span');
            if (span.className == '')
              span.removeAttribute('class');
            replaced = true;
          } else if (span.getStyle('fontWeight') == 'bold') {
            span.setStyle({fontWeight: ''});
            if (span.style.length == 0)
              span.removeAttribute('style');
            span.update('<strong>' + span.innerHTML + '</strong>');
            replaced = true;
          } else if (span.getStyle('fontStyle') == 'italic') {
            span.setStyle({fontStyle: ''});
            if (span.style.length == 0)
              span.removeAttribute('style');
            span.update('<em>' + span.innerHTML + '</em>');
            replaced = true;
          } else if (span.getStyle('textDecoration') == 'underline') {
            span.setStyle({textDecoration: ''});
            if (span.style.length == 0)
              span.removeAttribute('style');
            span.update('<u>' + span.innerHTML + '</u>');
            replaced = true;
          } else if (span.attributes.length == 0) {
            span.replace(span.innerHTML);
            replaced = true;
          }
        });
      } while (replaced);

    }

    var acceptableBlankTags = $A(['BR', 'IMG']);

    for (var i = 0; i < element.descendants().length; i++) {
      var node = element.descendants()[i];
      if (node.innerHTML.blank() && !acceptableBlankTags.include(node.nodeName) && node.id != 'bookmark')
        node.remove();
    }

    text = element.innerHTML;
    text = text.tidyXHTML();

    text = text.replace(/<br \/>(\n)*/g, "<br />\n");
    text = text.replace(/<\/p>\n<p>/g, "</p>\n\n<p>");

    text = text.replace(/<p>\s*<\/p>/g, "");

    text = text.replace(/\s*$/g, "");

    return text;
  }

  function formatHTMLInput() {
    var text = String(this);

    var element = Element("body");
    element.innerHTML = text;

    if (Prototype.Browser.Gecko || Prototype.Browser.WebKit) {
      element.select('strong').each(function(element) {
        element.replace('<span style="font-weight: bold;">' + element.innerHTML + '</span>');
      });
      element.select('em').each(function(element) {
        element.replace('<span style="font-style: italic;">' + element.innerHTML + '</span>');
      });
      element.select('u').each(function(element) {
        element.replace('<span style="text-decoration: underline;">' + element.innerHTML + '</span>');
      });
    }

    if (Prototype.Browser.WebKit)
      element.select('span').each(function(span) {
        if (span.getStyle('fontWeight') == 'bold')
          span.addClassName('Apple-style-span');

        if (span.getStyle('fontStyle') == 'italic')
          span.addClassName('Apple-style-span');

        if (span.getStyle('textDecoration') == 'underline')
          span.addClassName('Apple-style-span');
      });

    text = element.innerHTML;
    text = text.tidyXHTML();

    text = text.replace(/<\/p>(\n)*<p>/g, "\n\n");

    text = text.replace(/(\n)?<br( \/)?>(\n)?/g, "\n");

    text = text.replace(/^<p>/g, '');
    text = text.replace(/<\/p>$/g, '');

    if (Prototype.Browser.Gecko) {
      text = text.replace(/\n/g, "<br>");
      text = text + '<br>';
    } else if (Prototype.Browser.WebKit) {
      text = text.replace(/\n/g, "</div><div>");
      text = '<div>' + text + '</div>';
      text = text.replace(/<div><\/div>/g, "<div><br></div>");
    } else if (Prototype.Browser.IE || Prototype.Browser.Opera) {
      text = text.replace(/\n/g, "</p>\n<p>");
      text = '<p>' + text + '</p>';
      text = text.replace(/<p><\/p>/g, "<p>&nbsp;</p>");
      text = text.replace(/(<p>&nbsp;<\/p>)+$/g, "");
    }

    return text;
  }

  function tidyXHTML() {
    var text = String(this);

    text = text.gsub(/\r\n?/, "\n");

    text = text.gsub(/<([A-Z]+)([^>]*)>/, function(match) {
      return '<' + match[1].toLowerCase() + match[2] + '>';
    });

    text = text.gsub(/<\/([A-Z]+)>/, function(match) {
      return '</' + match[1].toLowerCase() + '>';
    });

    text = text.replace(/<br>/g, "<br />");

    return text;
  }

  return {
    formatHTMLOutput: formatHTMLOutput,
    formatHTMLInput:  formatHTMLInput,
    tidyXHTML:        tidyXHTML
  };
})());
Object.extend(String.prototype, {
  sanitize: function(options) {
    return Element("div").update(this).sanitize(options).innerHTML.tidyXHTML();
  }
});

Element.addMethods({
  sanitize: function(element, options) {
    element = $(element);
    options = $H(options);
    var allowed_tags = $A(options.get('tags') || []);
    var allowed_attributes = $A(options.get('attributes') || []);
    var sanitized = Element(element.nodeName);

    $A(element.childNodes).each(function(child) {
      if (child.nodeType == 1) {
        var children = $(child).sanitize(options).childNodes;

        if (allowed_tags.include(child.nodeName.toLowerCase())) {
          var new_child = Element(child.nodeName);
          allowed_attributes.each(function(attribute) {
            if ((value = child.readAttribute(attribute)))
              new_child.writeAttribute(attribute, value);
          });
          sanitized.appendChild(new_child);

          $A(children).each(function(grandchild) { new_child.appendChild(grandchild); });
        } else {
          $A(children).each(function(grandchild) { sanitized.appendChild(grandchild); });
        }
      } else if (child.nodeType == 3) {
        sanitized.appendChild(child);
      }
    });
    return sanitized;
  }
});

if (Prototype.Browser.IE) {
  function Range(ownerDocument) {
    this.ownerDocument = ownerDocument;

    this.startContainer = this.ownerDocument.documentElement;
    this.startOffset    = 0;
    this.endContainer   = this.ownerDocument.documentElement;
    this.endOffset      = 0;

    this.collapsed = true;
    this.commonAncestorContainer = null;

    this.START_TO_START = 0;
    this.START_TO_END   = 1;
    this.END_TO_END     = 2;
    this.END_TO_START   = 3;
  }

  document.createRange = function() {
    return new Range(this);
  };

  Object.extend(Range.prototype, {
    setStart: function(parent, offset) {},
    setEnd: function(parent, offset) {},
    setStartBefore: function(node) {},
    setStartAfter: function(node) {},
    setEndBefore: function(node) {},
    setEndAfter: function(node) {},

    collapse: function(toStart) {},

    selectNode: function(n) {},
    selectNodeContents: function(n) {},

    compareBoundaryPoints: function(how, sourceRange) {},

    deleteContents: function() {},
    extractContents: function() {},
    cloneContents: function() {},

    insertNode: function(n) {
      var range = this.ownerDocument.selection.createRange();
      var parent = this.ownerDocument.createElement('div');
      parent.appendChild(n);
      range.collapse();
      range.pasteHTML(parent.innerHTML);
    },
    surroundContents: function(newParent) {
      var range = this.ownerDocument.selection.createRange();
      var parent = this.document.createElement('div');
      parent.appendChild(newParent);
      node.innerHTML += range.htmlText;
      range.pasteHTML(parent.innerHTML);
    },

    cloneRange: function() {},
    toString: function() {},
    detach: function() {}
  });
}
WysiHat.Selection = Class.create((function() {
  function initialize(editor) {
    this.window = editor.getWindow();
    this.document = editor.getDocument();
  }

  function getSelection() {
    return this.window.getSelection ? this.window.getSelection() : this.document.selection;
  }

  function getRange() {
    var selection = this.getSelection();

    try {
      var range;
      if (selection.getRangeAt)
        range = selection.getRangeAt(0);
      else
        range = selection.createRange();
    } catch(e) { return null; }

    if (Prototype.Browser.WebKit) {
      range.setStart(selection.baseNode, selection.baseOffset);
      range.setEnd(selection.extentNode, selection.extentOffset);
    }

    return range;
  }

  function selectNode(node) {
    var selection = this.getSelection();

    if (Prototype.Browser.IE) {
      var range = createRangeFromElement(this.document, node);
      range.select();
    } else if (Prototype.Browser.WebKit) {
      selection.setBaseAndExtent(node, 0, node, node.innerText.length);
    } else if (Prototype.Browser.Opera) {
      range = this.document.createRange();
      range.selectNode(node);
      selection.removeAllRanges();
      selection.addRange(range);
    } else {
      var range = createRangeFromElement(this.document, node);
      selection.removeAllRanges();
      selection.addRange(range);
    }
  }

  function getNode() {
    var nodes = null, candidates = [], children, el;
    var range = this.getRange();

    if (!range)
      return null;

    var parent;
    if (range.parentElement)
      parent = range.parentElement();
    else
      parent = range.commonAncestorContainer;

    if (parent) {
      while (parent.nodeType != 1) parent = parent.parentNode;
      if (parent.nodeName.toLowerCase() != "body") {
        el = parent;
        do {
          el = el.parentNode;
          candidates[candidates.length] = el;
        } while (el.nodeName.toLowerCase() != "body");
      }
      children = parent.all || parent.getElementsByTagName("*");
      for (var j = 0; j < children.length; j++)
        candidates[candidates.length] = children[j];
      nodes = [parent];
      for (var ii = 0, r2; ii < candidates.length; ii++) {
        r2 = createRangeFromElement(this.document, candidates[ii]);
        if (r2 && compareRanges(range, r2))
          nodes[nodes.length] = candidates[ii];
      }
    }

    return nodes.first();
  }

  function createRangeFromElement(document, node) {
    if (document.body.createTextRange) {
      var range = document.body.createTextRange();
      range.moveToElementText(node);
    } else if (document.createRange) {
      var range = document.createRange();
      range.selectNodeContents(node);
    }
    return range;
  }

  function compareRanges(r1, r2) {
    if (r1.compareEndPoints) {
      return !(
        r2.compareEndPoints('StartToStart', r1) == 1 &&
        r2.compareEndPoints('EndToEnd', r1) == 1 &&
        r2.compareEndPoints('StartToEnd', r1) == 1 &&
        r2.compareEndPoints('EndToStart', r1) == 1
        ||
        r2.compareEndPoints('StartToStart', r1) == -1 &&
        r2.compareEndPoints('EndToEnd', r1) == -1 &&
        r2.compareEndPoints('StartToEnd', r1) == -1 &&
        r2.compareEndPoints('EndToStart', r1) == -1
      );
    } else if (r1.compareBoundaryPoints) {
      return !(
        r2.compareBoundaryPoints(0, r1) == 1 &&
        r2.compareBoundaryPoints(2, r1) == 1 &&
        r2.compareBoundaryPoints(1, r1) == 1 &&
        r2.compareBoundaryPoints(3, r1) == 1
        ||
        r2.compareBoundaryPoints(0, r1) == -1 &&
        r2.compareBoundaryPoints(2, r1) == -1 &&
        r2.compareBoundaryPoints(1, r1) == -1 &&
        r2.compareBoundaryPoints(3, r1) == -1
      );
    }

    return null;
  };

  function setBookmark() {
    var bookmark = this.document.getElementById('bookmark');
    if (bookmark)
      bookmark.parentNode.removeChild(bookmark);

    bookmark = this.document.createElement('span');
    bookmark.id = 'bookmark';
    bookmark.innerHTML = '&nbsp;';

    var range;
    if (Prototype.Browser.IE)
      range = new Range(this.document);
    else
      range = this.getRange();
    range.insertNode(bookmark);
  }

  function moveToBookmark() {
    var bookmark = this.document.getElementById('bookmark');
    if (!bookmark)
      return;

    if (Prototype.Browser.IE) {
      var range = this.getRange();
      range.moveToElementText(bookmark);
      range.collapse();
      range.select();
    } else if (Prototype.Browser.WebKit) {
      var selection = this.getSelection();
      selection.setBaseAndExtent(bookmark, 0, bookmark, 0);
    } else {
      var range = this.getRange();
      range.setStartBefore(bookmark);
    }

    bookmark.parentNode.removeChild(bookmark);
  }

  return {
    initialize:     initialize,
    getSelection:   getSelection,
    getRange:       getRange,
    getNode:        getNode,
    selectNode:     selectNode,
    setBookmark:    setBookmark,
    moveToBookmark: moveToBookmark
  };
})());

WysiHat.Toolbar = Class.create((function() {
  function initialize(editArea, options) {
    options = $H(options);

    this.editArea = editArea;

    this.hasMouseDown = false;
    this.element = new Element('div', { 'class': 'editor_toolbar' });

    var toolbar = this;
    this.element.observe('mousedown', function(event) {
      toolbar.mouseDown(event);
    });
    this.element.observe('mouseup', function(event) {
      toolbar.mouseUp(event);
    });

    insertToolbar(this, options);

    var buttonSet = options.get('buttonSet');
    if (buttonSet)
      this.addButtonSet(buttonSet);
  }

  function insertToolbar(toolbar, options) {
    var position = options.get('position') || 'before';
    var container = options.get('container') || toolbar.editArea;

    var insertOptions = $H({});
    insertOptions.set(position, toolbar.element);
    $(container).insert(insertOptions.toObject());
  }

  function addButtonSet(set) {
    var toolbar = this;
    $A(set).each(function(button) {
      var options = button.first();
      var handler = button.last();
      toolbar.addButton(options, handler);
    });

    return this;
  }

  function addButton(options, handler) {
    options = $H(options);
    var button = Element('a', { 'class': 'button', 'href': '#' }).update('<span>' + options.get('label') + '</span>');
    button.addClassName(options.get('name'));

    this.observeButtonClick(button, handler);
    this.observeStateChanges(button, options.get('name'));
    this.element.appendChild(button);

    return this;
  }

  function observeButtonClick(element, handler) {
    var toolbar = this;
    $(element).observe('click', function(event) {
      toolbar.hasMouseDown = true;
      handler(toolbar.editArea);
      toolbar.editArea.fire("wysihat:change");
      Event.stop(event);
      toolbar.hasMouseDown = false;
    });
    return this;
  }

  function observeStateChanges(element, command) {
    this.editArea.observe("wysihat:mousemove", function(event) {
      if (event.target.queryCommandState(command))
        element.addClassName('selected');
      else
        element.removeClassName('selected');
    });
    return this;
  }

  function mouseDown(event) {
    this.hasMouseDown = true;
  }

  function mouseUp(event) {
    this.editArea.focus();
    this.hasMouseDown = false;
  }

  return {
    initialize:          initialize,
    addButtonSet:        addButtonSet,
    addButton:           addButton,
    observeButtonClick:  observeButtonClick,
    observeStateChanges: observeStateChanges,
    mouseDown:           mouseDown,
    mouseUp:             mouseUp
  };
})());

WysiHat.Toolbar.ButtonSets = {};

WysiHat.Toolbar.ButtonSets.Basic = $A([
  [{ name: 'bold', label: "Bold" }, function(editor) {
    editor.boldSelection();
  }],

  [{ name: 'underline', label: "Underline" }, function(editor) {
    editor.underlineSelection();
  }],

  [{ name: 'italic', label: "Italic" }, function(editor) {
    editor.italicSelection();
  }]
]);
