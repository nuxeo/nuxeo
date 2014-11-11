

def extractToken(text, tag_start, tag_end):
    start = text.find(tag_start) + len(tag_start)
    end = text.find(tag_end, start)
    if start < len(tag_start) or end < 0:
        return None
    return text[start:end]


def extractJsfState(html):
    state = extractToken(html, '<input type="hidden" name="javax.faces.ViewState"'\
                             ' id="j_id1:javax.faces.ViewState:0" value="', '"')
    if not state:
        # try to extract state from partial ajax response
        state = extractToken(html, '<update id="j_id1:javax.faces.ViewState:0"><![CDATA[',
                             ']]></update>')
    if not state:
        raise ValueError('No JSF state found in the page.')
    return state


def extractIframes(body):
    ifr_tag = '/nuxeo/opensocial/gadgets/ifr?'
    items = body.split(ifr_tag)
    iframes = [ifr_tag[6:] + item[:item.find('"')] for item in items[1:]]
    return iframes


def extractJsessionId(fl):
    jid = None
    for dom, cookies in fl._browser.cookies.items():
        for path, cookie in cookies.items():
            if not cookie.has_key('JSESSIONID'):
                continue
            jid = cookie['JSESSIONID'].coded_value
    return jid
