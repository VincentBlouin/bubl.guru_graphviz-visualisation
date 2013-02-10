package org.triple_brain.module.graphviz_visualisation;
/*
 * Copyright Mozilla Public License 1.1
 */

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.triple_brain.module.model.graph.SubGraph;
import org.triple_brain.module.model.json.drawn_graph.DrawnVertexJSONFields;
import org.triple_brain.module.model.json.graph.EdgeJsonFields;
import org.triple_brain.module.model.json.graph.VertexJsonFields;

import java.util.regex.Pattern;

import static org.triple_brain.module.model.json.drawn_graph.DrawnEdgeJSONFields.*;
import static org.triple_brain.module.model.json.drawn_graph.DrawnGraphJSONFields.BOUNDING_BOX_HEIGHT;
import static org.triple_brain.module.model.json.drawn_graph.DrawnGraphJSONFields.BOUNDING_BOX_WIDTH;
import static org.triple_brain.module.model.json.drawn_graph.PointJSONFields.X;
import static org.triple_brain.module.model.json.drawn_graph.PointJSONFields.Y;
import static org.triple_brain.module.model.json.graph.GraphJSONFields.EDGES;
import static org.triple_brain.module.model.json.graph.GraphJSONFields.VERTICES;

public class GraphVizToJsonGraph {

    public static final Pattern REAL_NUMBER = Pattern.compile("[0-9]+(\\.[0-9]+)?");

    private SubGraph originalGraph;
    private String graphvizDot = "";


    JSONObject builtGraph = new JSONObject();

    public static GraphVizToJsonGraph withOriginalGraphAndGraphvizString(
            SubGraph originalGraph, String graphvizDot){
        return new GraphVizToJsonGraph(originalGraph, graphvizDot);
    }

    protected GraphVizToJsonGraph(SubGraph originalGraph, String graphvizDot){
        this.originalGraph = originalGraph;
        this.graphvizDot = graphvizDot;
    }


    //The bounding box is the box around the graph. In graphviz dot it's found next to the 'bb' attribute of the graph
    Pattern boundingBox = Pattern.compile("bb");
    //now setting a number of patterns usefull for later use within this method
    //each edge or vertex description of the dot format are encapsulated within '[' and ']'
    Pattern bezierAttributePattern = Pattern.compile("B");
    Pattern nextDescription = Pattern.compile("\\[");
    Pattern endDescription = Pattern.compile("[^\\]]*");
    Pattern labelPositionPattern = Pattern.compile("[.]*lp=");
    Pattern arrowHeadDrawingInfo = Pattern.compile("_hdraw_=");
    Pattern vertexPos = Pattern.compile("pos=");
    Pattern lDraw = Pattern.compile("_ldraw_");
    Pattern vertexLabelPattern = Pattern.compile("[a-z]\\s[0-9]\\s-[^-]+[-]");
    Pattern edgeLabelName = Pattern.compile("[a-z]\\s[0-9]\\s-[^-]+[-]");
    Pattern arrowPositionPattern = Pattern.compile("P\\s3");
    //each vertex and edge have an additionnal attribute added which is id.
    Pattern idPropertyPattern = Pattern.compile("id=");
    Pattern idValuePattern = Pattern.compile("[.|_|-|\\/]*,");
    //each vertex and edge have an additionnal attribute added which is is_vertex. is_vertex=1 means it's a vertex and is_vertex=0 means it's an edge
    Pattern relationPattern = Pattern.compile("->");

    Pattern centerVertexPattern = Pattern.compile("is_center_vertex=true");

    public JSONObject convert()throws JSONException{
        builtGraph.put(VERTICES, new JSONObject());
        builtGraph.put(EDGES, new JSONArray());
        StringScanner mainStringScanner = StringScanner.withTextToParseAndCurrentPattern(graphvizDot, boundingBox);
        //the text before the bounding box and the attribute 'bb' is useless for us. so we use scanner.next()
        mainStringScanner.next();
        mainStringScanner.pattern(REAL_NUMBER);
        //the third number after the 'bb' attribute corresponds to the bounding box width
        builtGraph.put(BOUNDING_BOX_WIDTH, mainStringScanner.next(3));
        //the next number is the height of the bounding box
        builtGraph.put(BOUNDING_BOX_HEIGHT, mainStringScanner.next());

        Boolean isEdge;
        String sourceVertexId = "";
        String destinationVertexId = "";

        mainStringScanner.pattern(nextDescription);
        //while there is draw information
        while (!mainStringScanner.next().equalsIgnoreCase("")) {
            mainStringScanner.pattern(idPropertyPattern);
            String relationOrVertexId = mainStringScanner.pattern(idPropertyPattern).lastRemovedText();
            StringScanner scannerForEdge = StringScanner.withTextToParseAndCurrentPattern(relationOrVertexId, relationPattern);
            isEdge = !scannerForEdge.next().equals("");
            if (isEdge) {
                sourceVertexId = relationOrVertexId.split("->")[0].split(";")[1].trim();
                destinationVertexId = relationOrVertexId.split("->")[1].trim();
            }

            mainStringScanner.next();
            mainStringScanner.pattern(idValuePattern);
            //skipping to what ends the element id value
            mainStringScanner.next();
            String elementId = mainStringScanner.lastRemovedText().replace("\"", "");

            if (isEdge) {
                JSONObject edge = EdgeJsonFields.toJson(
                        originalGraph.edgeWithIdentifier(elementId)
                );

                //scanning for edge label position
                mainStringScanner.pattern(labelPositionPattern);
                mainStringScanner.next();
                mainStringScanner.pattern(REAL_NUMBER);
                Double labelPosX = new Double(mainStringScanner.next());
                Double labelPosY = new Double(mainStringScanner.next());
                JSONObject labelPosition = new JSONObject();
                labelPosition.put(X, labelPosX);
                labelPosition.put(Y, labelPosY);
                edge.put(LABEL_POSITION, labelPosition);

                /*
                * Scanning for the arrow line bezier points.
                * The number of bezier points is unknown previously to calling graphViz.
                * The arrow line bezier points are all before the arrow head drawing info.
                * Therefore, we get all the text before the arrow head drawing info and then we scan for all numbers within that retrieved text.
                */
                mainStringScanner.pattern(arrowHeadDrawingInfo);
                mainStringScanner.next();
                String arrowLineBezierPoints = mainStringScanner.lastRemovedText();
                StringScanner arrowLineBezierPointsScanner = StringScanner.withTextToParseAndCurrentPattern(arrowLineBezierPoints, bezierAttributePattern);
                //scan up to the bezier attribute
                arrowLineBezierPointsScanner.next();
                arrowLineBezierPointsScanner.pattern(REAL_NUMBER);
                //Skipping the next number about the number of bezier points
                arrowLineBezierPointsScanner.next();
                edge.put(ARROW_LINE_BEZIER_POINTS, new JSONArray());
                String arrowLineBezierPointXPositionStr = arrowLineBezierPointsScanner.next();
                while (!arrowLineBezierPointXPositionStr.equalsIgnoreCase("")) {
                    String arrowLineBezierPointYPositionStr = arrowLineBezierPointsScanner.next();
                    JSONObject arrowLineBezierPoint = new JSONObject();
                    arrowLineBezierPoint.put(X, new Double(arrowLineBezierPointXPositionStr));
                    arrowLineBezierPoint.put(Y, new Double(arrowLineBezierPointYPositionStr));
                    edge.getJSONArray(ARROW_LINE_BEZIER_POINTS).put(arrowLineBezierPoint);
                    arrowLineBezierPointXPositionStr = arrowLineBezierPointsScanner.next();
                }

                //scanning for the edge arrow head triangle 3 summit points
                mainStringScanner.pattern(arrowPositionPattern);
                mainStringScanner.next();
                mainStringScanner.pattern(REAL_NUMBER);
                JSONObject arrowHeadPoint1 = new JSONObject();
                arrowHeadPoint1.put(X, mainStringScanner.next());
                arrowHeadPoint1.put(Y, mainStringScanner.next());
                JSONObject arrowHeadPoint2 = new JSONObject();
                arrowHeadPoint2.put(X, mainStringScanner.next());
                arrowHeadPoint2.put(Y, mainStringScanner.next());
                JSONObject arrowHeadPoint3 = new JSONObject();
                arrowHeadPoint3.put(X, mainStringScanner.next());
                arrowHeadPoint3.put(Y, mainStringScanner.next());
                edge.put(ARROW_HEAD_SUMMIT_1, arrowHeadPoint1);
                edge.put(ARROW_HEAD_SUMMIT_2, arrowHeadPoint2);
                edge.put(ARROW_HEAD_SUMMIT_3, arrowHeadPoint3);

                //scanning for the edge label name
                mainStringScanner.pattern(edgeLabelName);
                mainStringScanner.next();
                mainStringScanner.pattern(endDescription);
                String edgeLabel = mainStringScanner.next();

                //adding the newly created edge to the built graph
                builtGraph.getJSONArray(EDGES).put(edge);
            } else {
                JSONObject jsonVertex = VertexJsonFields.toJson(
                        originalGraph.vertexWithIdentifier(elementId)
                );

                //scanning for the vertex position
                mainStringScanner.pattern(vertexPos);
                mainStringScanner.next();

                mainStringScanner.pattern(REAL_NUMBER);
                Double xPosition = new Double(mainStringScanner.next());
                Double yPosition = new Double(mainStringScanner.next());
                JSONObject position = new JSONObject();
                position.put(X, xPosition);
                position.put(Y, yPosition);
                jsonVertex.put(DrawnVertexJSONFields.POSITION, position);

                //scanning for the vertex width and height
                mainStringScanner.pattern(REAL_NUMBER);
                jsonVertex.put(DrawnVertexJSONFields.WIDTH, mainStringScanner.next());
                jsonVertex.put(DrawnVertexJSONFields.HEIGHT, mainStringScanner.next());

                //scanning for the vertex label
                mainStringScanner.pattern(lDraw);
                mainStringScanner.next();
                mainStringScanner.pattern(vertexLabelPattern);
                mainStringScanner.next();
                //The label is just before the end of the vertex description. The end of a description is characterized by ']'
                mainStringScanner.pattern(endDescription);
                String vertexLabel = mainStringScanner.next();
                //removing 2 extras characters got from the last scanner.next();
                vertexLabel = vertexLabel.substring(0, vertexLabel.length() - 2);

                //adding the newly created vertex to the built graph
                builtGraph.getJSONObject(VERTICES).put(elementId, jsonVertex);
            }

            mainStringScanner.pattern(nextDescription);
        }
        return builtGraph;
    }
}
