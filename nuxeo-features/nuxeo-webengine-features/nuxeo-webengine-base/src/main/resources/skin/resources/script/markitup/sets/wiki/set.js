//MarkitUp set for the WikiModel common syntax

myWikiSettings = {
    nameSpace:          "wiki", // Useful to prevent multi-instances CSS conflict
    previewParserPath:  document.location.pathname+"/@views/preview",
    previewParserVar: 'wiki_editor',
    previewAutorefresh: true,
    previewInWindow: 'width=500, height=700, resizable=yes, scrollbars=yes',
    onShiftEnter:       {keepDefault:false, replaceWith:'\n\n'},
    markupSet:  [
        {name:'Heading 1', key:'1', openWith:'== ', closeWith:' ==', placeHolder:'Your title here...' },
        {name:'Heading 2', key:'2', openWith:'=== ', closeWith:' ===', placeHolder:'Your title here...' },
        {name:'Heading 3', key:'3', openWith:'==== ', closeWith:' ====', placeHolder:'Your title here...' },
        {name:'Heading 4', key:'4', openWith:'===== ', closeWith:' =====', placeHolder:'Your title here...' },
        {name:'Heading 5', key:'5', openWith:'====== ', closeWith:' ======', placeHolder:'Your title here...' },
        {separator:'---------------' },
        {name:'Bold', key:'B', openWith:"**", closeWith:"**"},
        {name:'Italic', key:'I', openWith:"__", closeWith:"__"},
        //{name:'Stroke through', key:'S', openWith:'<s>', closeWith:'</s>'},
        {separator:'---------------' },
        {name:'Bulleted list', openWith:'(!(- |!|-)!)'},
        {name:'Numeric list', openWith:'(!(+ |!|+)!)'},
        {separator:'---------------' },
        {name:'Image', key:"T", replaceWith:'[[Image:[![Url:!:http://]!]|[![name]!]]]'},
        {name:'Link', key:"L", openWith:"[[![Link]!] ", closeWith:']', placeHolder:'Your text to link here...' },
        {name:'Url', openWith:"[[![Url:!:http://]!] ", closeWith:']', placeHolder:'Your text to link here...' },
        {separator:'---------------' },
        {name:'Quotes', openWith:'(!(> |!|>)!)'},
        {name:'Inline Code', openWith:'$$', closeWith:'$$'},
        {name:'Code', openWith:'{{{', closeWith:'}}}'},
        {separator:'---------------' },
        {name:'Preview', key: 'P', call:'preview', className:'preview'}
    ]
};
