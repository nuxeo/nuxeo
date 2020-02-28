# coding: utf-8
import datetime
import igraph as ig
import json

import chart_studio.plotly as py
import plotly.graph_objs as go

parent_path = "../nuxeo-apidoc-repo/src/test/resources/"
fnames = [
    "gephi.json",
    "complete_graph_ref.json",
    "complete_bundles_graph_ref.json",
    "bundle_graph.json"
    ]

def generate_graph(parent_path, fname):
    f = open(parent_path + fname, "r")
    data = json.loads(f.read())
    
    N=len(data['nodes'])
    L=len(data['edges'])
    Edges=[(data['edges'][k]['source'], data['edges'][k]['target']) for k in range(L)]
    
    G=ig.Graph(Edges, directed=False)
    
    labels=[]
    group=[]
    nodes_by_key = {}
    for node in data['nodes']:
        nodes_by_key[node['id']] = node
        label = '<b>{label}</b><br />Type: {type}<br />Category: {category}<br />Weight: {weight}<br />x: {x}<br />y: {y}<br /> z:{z}'.format(**node)
        labels.append(label)
        if node['color']:
            group.append(node['color'])
    
    #layt=G.layout('dag', dim=3)
    #Xn=[layt[k][0] for k in range(N)]# x-coordinates of nodes
    #Yn=[layt[k][1] for k in range(N)]# y-coordinates
    #Zn=[layt[k][2] for k in range(N)]# z-coordinates
    #Xe=[]
    #Ye=[]
    #Ze=[]
    #for e in Edges:
    #    Xe+=[layt[e[0]][0],layt[e[1]][0], None]# x-coordinates of edge ends
    #    Ye+=[layt[e[0]][1],layt[e[1]][1], None]
    #    Ze+=[layt[e[0]][2],layt[e[1]][2], None]
    
    Xn=[data['nodes'][k]['x'] for k in range(N)]# x-coordinates of nodes
    Yn=[data['nodes'][k]['y'] for k in range(N)]# y-coordinates
    Zn=[data['nodes'][k]['z'] for k in range(N)]# z-coordinates
    Xe=[]
    Ye=[]
    Ze=[]
    for e in Edges:
        s = nodes_by_key[e[0]]
        t = nodes_by_key[e[1]]
        Xe+=[s['x'], t['x'], None]# x-coordinates of edge ends
        Ye+=[s['y'], t['y'], None]
        Ze+=[s['z'], t['z'], None]
    
    
    trace1=go.Scatter3d(x=Xe,
                   y=Ye,
                   z=Ze,
                   mode='lines',
                   line=dict(color='rgb(125,125,125)', width=1),
                   hoverinfo='none'
                   )
    
    trace2=go.Scatter3d(x=Xn,
                   y=Yn,
                   z=Zn,
                   mode='markers',
                   name='nodes',
                   marker=dict(symbol='circle',
                                 size=6,
                                 color=group,
                                 colorscale='Viridis',
                                 line=dict(color='rgb(50,50,50)', width=0.5)
                                 ),
                   text=labels,
                   hoverinfo='text'
                   )
    
    axis=dict(showbackground=True,
              showline=True,
              zeroline=True,
              showgrid=True,
              showticklabels=True,
              title=''
              )
    
    layout = go.Layout(
             title="Nuxeo Bundles",
             width=1000,
             height=1000,
             showlegend=False,
             scene=dict(
                 xaxis=dict(axis),
                 yaxis=dict(axis),
                 zaxis=dict(axis),
            ),
         margin=dict(
            t=100
        ),
        hovermode='closest',
        annotations=[
               dict(
               showarrow=False,
                text="Blah",
                xref='paper',
                yref='paper',
                x=0,
                y=0.1,
                xanchor='left',
                yanchor='bottom',
                font=dict(
                size=14
                )
                )
            ],    )
    
    
    data=[trace1, trace2]
    fig=go.Figure(data=data, layout=layout)
    
    now = datetime.datetime.now().strftime("%Y-%m-%d_%H:%M")
    fig.write_html("%s_%s.html" % (fname, now))
  
for fname in fnames:
    generate_graph(parent_path, fname)
