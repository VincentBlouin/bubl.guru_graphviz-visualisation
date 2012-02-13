package org.triple_brain.module.graphviz_visualisation;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.ovea.tadjin.util.Uuid;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.triple_brain.model.json.drawn_graph.DrawnEdgeJSONFields;
import org.triple_brain.model.json.drawn_graph.DrawnVertexJSONFields;
import org.triple_brain.model.json.graph.EdgeJSONFields;
import org.triple_brain.model.json.graph.VertexJSONFields;
import org.triple_brain.module.graphviz_visualisation.draw.DrawConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.hp.hpl.jena.vocabulary.RDFS.label;
import static org.triple_brain.graphmanipulator.jena.TripleBrainModel.*;
import static org.triple_brain.model.json.drawn_graph.DrawnEdgeJSONFields.*;
import static org.triple_brain.model.json.drawn_graph.DrawnGraphJSONFields.*;
import static org.triple_brain.model.json.drawn_graph.PointJSONFields.*;
import static org.triple_brain.model.json.graph.VertexJSONFields.*;

/**
 * @author Vincent Blouin
 */
public class JenaGraphToDrawnGraphConverter {

    public static final Pattern REAL_NUMBER = Pattern.compile("[0-9]+(\\.[0-9]+)?");

    public static final String GRAPH_VIZ_TEMP_FILES_RELATIVE_FOLDER_PATH = "./";

    public static JSONObject graphVizDrawing(Model jenaGraph) throws JSONException {
        Map<String, Resource> verticesAndEdges = new HashMap<String, Resource>();
        String notDrawnGraphVizFormat = convertFromJenaToNotDrawnGraphViz(jenaGraph, verticesAndEdges);
        String graphVizDrawnGraph = drawnGraph(notDrawnGraphVizFormat);
        return convertDrawnGraphToJSON(graphVizDrawnGraph, verticesAndEdges);
    }

    private static String convertFromJenaToNotDrawnGraphViz(Model jenaGraph, Map<String, Resource> verticesAndEdges) {
        String notDrawnGraphVizDot = "digraph G {node [fontsize=\"" + DrawConstants.VERTEX_FONT_SIZE + "\",fontname=\"" + DrawConstants.VERTEX_FONT_FAMILY + "\"]; edge [fontsize=\"" + DrawConstants.EDGE_FONT_SIZE + "\",fontname=\"" + DrawConstants.EDGE_FONT_FAMILY + "\"]; K=0.85 mode=\"ipsep\"; sep=\"+35\"; ";
        String verticesInGraphVizDot = "";
        String edgesInGraphVizDot = "";

        List<Resource> addedSubjects = new ArrayList<Resource>();
        List<Property> addedPredicates = new ArrayList<Property>();
        List<Object> addedObjects = new ArrayList<Object>();

        for (Statement statement : jenaGraph.listStatements().toList()) {
            if(isResourceAVertexOrEdge(statement.getSubject())){
                String subjectLabel = statement.getSubject().getProperty(label).getString();
                if (subjectLabel.equalsIgnoreCase("")) {
                    subjectLabel = EMPTY_VERTEX_LABEL;
                }
                String subjectLocalName = statement.getSubject().getLocalName();
                Property subjectAsProperty = jenaGraph.getProperty(statement.getSubject().getURI());

                Integer numberOfStatementsWithSubjectAsProperty = jenaGraph.listSubjectsWithProperty(subjectAsProperty).toList().size();
                Boolean subjectIsVertex = numberOfStatementsWithSubjectAsProperty == 0;

                if (!addedSubjects.contains(statement.getSubject()) && subjectIsVertex) {
                    String vertexInGraphVizDot = subjectLocalName + "[" + VertexJSONFields.ID + "=\"" + subjectLocalName + "\" " + VertexJSONFields.LABEL + "=\"" + subjectLabel + "\"]";
                    verticesInGraphVizDot += vertexInGraphVizDot;
                    addedSubjects.add(statement.getSubject());
                    verticesAndEdges.put(statement.getSubject().getLocalName(), statement.getSubject());
                }

                if (isRelationToBeDrawnInTheGraph(statement.getPredicate())) {

                    String objectLabel = statement.getObject().asResource().getProperty(label).getString();
                    if (objectLabel.equalsIgnoreCase("")) {
                        objectLabel = EMPTY_VERTEX_LABEL;
                    }
                    String objectLocalName = statement.getObject().asResource().getLocalName();

                    if (!addedObjects.contains(statement.getObject())) {
                        String vertexInGraphVizDot = objectLocalName + "[" + VertexJSONFields.ID + "=\"" + objectLocalName + "\" " + VertexJSONFields.LABEL + "=\"" + objectLabel + "\"]";
                        verticesInGraphVizDot += vertexInGraphVizDot;
                        addedObjects.add(statement.getObject());
                        verticesAndEdges.put(statement.getObject().asResource().getLocalName(), statement.getObject().asResource());
                    }

                    if (!addedPredicates.contains(statement.getPredicate())) {
                        String predicateLabel = statement.getPredicate().asResource().getProperty(label).getString();
                        if (predicateLabel.equalsIgnoreCase("")) {
                            predicateLabel = EMPTY_EDGE_LABEL;
                        }
                        String predicateLocalName = statement.getPredicate().asResource().getLocalName();
                        edgesInGraphVizDot += subjectLocalName + "->" + objectLocalName + "["+ EdgeJSONFields.ID+"=\"" + predicateLocalName + "\" "+ EdgeJSONFields.LABEL+"=\"" + predicateLabel + "\"];";
                        addedPredicates.add(statement.getPredicate());
                        verticesAndEdges.put(statement.getPredicate().getLocalName(), statement.getPredicate());
                    }
                }
            }
        }
        notDrawnGraphVizDot += verticesInGraphVizDot;
        notDrawnGraphVizDot += edgesInGraphVizDot;
        notDrawnGraphVizDot += "}";
        return notDrawnGraphVizDot;
    }

    private static Boolean isResourceAVertexOrEdge(Resource resource) {
        return resource.hasProperty(label);
    }

    private static Boolean isRelationToBeDrawnInTheGraph(Property property) {
        return property.hasProperty(label);
    }

    private static String drawnGraph(String sourceGraph) {

        byte bytes[] = graphFromGraphViz(GRAPH_VIZ_TEMP_FILES_RELATIVE_FOLDER_PATH, sourceGraph, "xdot");
        return new String(bytes);
    }

    /*
    * Returns the graph as an image in binary format.
    * @param dot_source Source of the graph to be drawn.
    * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
    * @return A byte array containing the image of the graph.
    */
    private static byte[] graphFromGraphViz(String tempFolder, String dot_source, String type) {
        File dot;
        byte[] img_stream = null;

        try {
            dot = writeDotSourceToFile(tempFolder, dot_source);
            if (dot != null) {
                img_stream = get_img_stream(tempFolder, dot, type);
                if (dot.delete() == false)
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    /**
     * It will call the external dot program, and return the image in
     * binary format.
     *
     * @param dot  Source of the graph (in dot language).
     * @param type Type of the output image to be produced, e.g.: gif, dot, fig, pdf, ps, svg, png.
     * @return The image of the graph in .gif format.
     */
    private static byte[] get_img_stream(String tempFolder, File dot, String type) {
        File img;
        byte[] img_stream = null;

        try {
            //modified by vince to write locally
            String pathToFile = tempFolder + "/";
            img = new File(pathToFile + Uuid.generate());
            Runtime rt = Runtime.getRuntime();

            String pathToGraphViz = isRunningOSWindows() ? "C:\\Program Files (x86)\\Graphviz 2.28/bin/dot.exe" : "/usr/bin/dot";
            // patch by Mike Chenault
            String[] args = {pathToGraphViz, "-T" + type, "-Kfdp", dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);

            p.waitFor();

            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            // Close it if we need to
            if (in != null) in.close();

            if (img.delete() == false)
                System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
        } catch (java.io.IOException ioe) {
            System.err.println("Error:    in I/O processing of tempfile in dir " + tempFolder + "\n");
            System.err.println("       or in calling external command");
            ioe.printStackTrace();
        } catch (java.lang.InterruptedException ie) {
            System.err.println("Error: the execution of the external program was interrupted");
            ie.printStackTrace();
        }

        return img_stream;
    }

    /**
     * Writes the source of the graph in a file, and returns the written file
     * as a File object.
     *
     * @param str Source of the graph (in dot language).
     * @return The file (as a File object) that contains the source of the graph.
     */
    private static File writeDotSourceToFile(String tempFolder, String str) throws java.io.IOException {
        File temp;
        try {

            String pathToFile = tempFolder + "/";
            temp = new File(pathToFile + Uuid.generate());
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
            fout.close();
        } catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            e.printStackTrace();
            return null;
        }
        return temp;
    }

    private static Boolean isRunningOSWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    private static JSONObject convertDrawnGraphToJSON(String graphVizDrawnGraphDot, Map<String, Resource> verticesAndEdges) throws JSONException {
        JSONObject builtGraph = new JSONObject();
        builtGraph.put(VERTICES, new JSONArray());
        builtGraph.put(EDGES, new JSONArray());
        //The bounding box is the box around the graph. In graphviz dot it's found next to the 'bb' attribute of the graph
        Pattern boundingBox = Pattern.compile("bb");
        StringScanner mainStringScanner = StringScanner.withTextToParseAndCurrentPattern(graphVizDrawnGraphDot, boundingBox);
        //the text before the bounding box and the attribute 'bb' is useless for us. so we use scanner.next()
        mainStringScanner.next();
        mainStringScanner.pattern(REAL_NUMBER);
        //the third number after the 'bb' attribute corresponds to the bounding box width
        builtGraph.put(BOUNDING_BOX_WIDTH, mainStringScanner.next(3));
        //the next number is the height of the bounding box
        builtGraph.put(BOUNDING_BOX_HEIGHT, mainStringScanner.next());

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
        Pattern idValuePattern = Pattern.compile("[.]*,");
        //each vertex and edge have an additionnal attribute added which is is_vertex. is_vertex=1 means it's a vertex and is_vertex=0 means it's an edge
        Pattern relationPattern = Pattern.compile("->");

        Pattern centerVertexPattern = Pattern.compile("is_center_vertex=true");

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
            String elementId = mainStringScanner.lastRemovedText();

            if (isEdge) {
                JSONObject edge = new JSONObject();
                edge.put(DrawnEdgeJSONFields.ID, elementId);
                edge.put(DrawnEdgeJSONFields.SOURCE_VERTEX_ID, sourceVertexId);
                edge.put(DrawnEdgeJSONFields.DESTINATION_VERTEX_ID, destinationVertexId);

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
                edgeLabel = edgeLabel.substring(0, edgeLabel.length() - 2);
                edge.put(DrawnEdgeJSONFields.LABEL, edgeLabel);

                //adding the newly created edge to the built graph
                builtGraph.getJSONArray(EDGES).put(edge);
            } else {
                JSONObject vertex = new JSONObject();
                vertex.put(DrawnVertexJSONFields.ID, elementId);

                Resource vertexAsResource = verticesAndEdges.get(elementId);
                Boolean isFrontierVertexWithHiddenVertices = vertexAsResource.hasProperty(IS_FRONTIER_VERTEX_WITH_HIDDEN_VERTICES());
                if(isFrontierVertexWithHiddenVertices){
                    vertex.put(VertexJSONFields.IS_FRONTIER_VERTEX_WITH_HIDDEN_VERTICES, true);
                    Integer numberOfHiddenConnectedVertices = vertexAsResource.getProperty(NUMBER_OF_HIDDEN_CONNECTED_VERTICES()).getInt();
                    vertex.put(VertexJSONFields.NUMBER_OF_HIDDEN_CONNECTED_VERTICES, numberOfHiddenConnectedVertices);
                    Seq hiddenPropertiesLabel = vertexAsResource.getProperty(NAME_OF_HIDDEN_PROPERTIES()).getObject().as(Seq.class);
                    JSONArray hiddenPropertiesOfVertex = new JSONArray();
                    for(int i = 1 ; i <= hiddenPropertiesLabel.size(); i++){
                        String propertyName = hiddenPropertiesLabel.getString(i);
                        if(propertyName.equals("")){
                            propertyName = EMPTY_EDGE_LABEL;
                        }
                        hiddenPropertiesOfVertex.put(propertyName);
                    }
                    vertex.put(NAME_OF_HIDDEN_PROPERTIES, hiddenPropertiesOfVertex);
                }
                Integer minNumberOfEdgesFromCenterVertex = vertexAsResource.getProperty(MIN_NUMBER_OF_EDGES_FROM_CENTER_VERTEX()).getInt();
                vertex.put(MIN_NUMBER_OF_EDGES_FROM_CENTER_VERTEX, minNumberOfEdgesFromCenterVertex);

                //scanning for the vertex position
                mainStringScanner.pattern(vertexPos);
                mainStringScanner.next();

                mainStringScanner.pattern(REAL_NUMBER);
                Double xPosition = new Double(mainStringScanner.next());
                Double yPosition = new Double(mainStringScanner.next());
                JSONObject position = new JSONObject();
                position.put(X, xPosition);
                position.put(Y, yPosition);
                vertex.put(DrawnVertexJSONFields.POSITION, position);

                //scanning for the vertex width and height
                mainStringScanner.pattern(REAL_NUMBER);
                vertex.put(DrawnVertexJSONFields.WIDTH, mainStringScanner.next());
                vertex.put(DrawnVertexJSONFields.HEIGHT, mainStringScanner.next());

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
                vertex.put(DrawnVertexJSONFields.LABEL, vertexLabel);

                //adding the newly created vertex to the built graph
                builtGraph.getJSONArray(VERTICES).put(vertex);
            }

            mainStringScanner.pattern(nextDescription);
        }
        return builtGraph;
    }
}
