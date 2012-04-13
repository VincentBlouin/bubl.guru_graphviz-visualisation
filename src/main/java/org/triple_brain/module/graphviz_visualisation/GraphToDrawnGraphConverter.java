package org.triple_brain.module.graphviz_visualisation;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.triple_brain.module.graphviz_visualisation.draw.DrawConstants;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Graph;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.model.json.graph.EdgeJSONFields;
import org.triple_brain.module.model.json.graph.VertexJSONFields;

import java.util.Set;
import static org.triple_brain.graphmanipulator.jena.TripleBrainModel.*;
/**
 * Copyright Mozilla Public License 1.1
 */
public class GraphToDrawnGraphConverter {

    private Graph graph;

    public static GraphToDrawnGraphConverter withGraph(Graph graph){
        return new GraphToDrawnGraphConverter(graph);
    }

    protected  GraphToDrawnGraphConverter(Graph graph){
        this.graph = graph;
    }

    public JSONObject convert() throws JSONException {
        String notDrawnGraphVizFormat = convertGraphToNotDrawnGraphViz();
        String graphVizDrawnGraph = new GraphvizService()
                .convertToDotDrawnGraph(notDrawnGraphVizFormat);
        return GraphVizToJsonGraph.withOriginalGraphAndGraphvizString(
                graph, graphVizDrawnGraph
                ).convert();
    }

    private String convertGraphToNotDrawnGraphViz() {
        String notDrawnGraphVizDot = "digraph G {node [fontsize=\"" + DrawConstants.VERTEX_FONT_SIZE + "\",fontname=\"" + DrawConstants.VERTEX_FONT_FAMILY + "\"]; edge [fontsize=\"" + DrawConstants.EDGE_FONT_SIZE + "\",fontname=\"" + DrawConstants.EDGE_FONT_FAMILY + "\"]; K=0.85 mode=\"ipsep\"; sep=\"+35\"; ";
        notDrawnGraphVizDot += verticesRepresentationInDot(graph.vertices());
        notDrawnGraphVizDot += edgesRepresentationInDot(graph.edges());
        notDrawnGraphVizDot += "}";
        return notDrawnGraphVizDot;
    }

    private String verticesRepresentationInDot(Set<Vertex> vertices){
        String verticesInGraphVizDot = "";
        for(Vertex vertex : vertices){
            verticesInGraphVizDot += vertexRepresentationInDot(vertex);
        }
        return verticesInGraphVizDot;
    }

    private String edgesRepresentationInDot(Set<Edge> edges){
        String edgesInGraphVizDot = "";
        for(Edge edge : edges){
            edgesInGraphVizDot += edgeRepresentationInDot(edge);
        }
        return edgesInGraphVizDot;
    }

    private String vertexRepresentationInDot(Vertex vertex){
        return "\"" + vertex.id() + "\" [" + VertexJSONFields.ID + "=\"" + vertex.id() + "\" " + VertexJSONFields.LABEL + "=\"" + (vertex.label().trim().equals("") ? EMPTY_VERTEX_LABEL: vertex.label()) + "\"]";

    }

    private String edgeRepresentationInDot(Edge edge){
        return "\"" + edge.sourceVertex().id() + "\"->\"" + edge.destinationVertex().id() + "\"["+ EdgeJSONFields.ID+"=\"" + edge.id() + "\" "+ EdgeJSONFields.LABEL+"=\"" + (edge.label().trim().equals("") ? EMPTY_EDGE_LABEL : edge.label()) + "\"];";

    }
}
