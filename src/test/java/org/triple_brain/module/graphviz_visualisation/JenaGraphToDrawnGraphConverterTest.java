package org.triple_brain.module.graphviz_visualisation;

import com.hp.hpl.jena.rdf.model.*;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.triple_brain.graphmanipulator.jena.graph.JenaEdgeManipulator;
import org.triple_brain.graphmanipulator.jena.graph.JenaGraphManipulator;
import org.triple_brain.graphmanipulator.jena.graph.JenaVertexManipulator;
import org.triple_brain.model.json.graph.EdgeJSONFields;
import org.triple_brain.model.json.graph.VertexJSONFields;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.triple_brain.graphmanipulator.jena.TripleBrainModel.*;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.triple_brain.model.json.drawn_graph.DrawnGraphJSONFields.*;
import static org.triple_brain.model.json.drawn_graph.DrawnVertexJSONFields.*;
import static org.triple_brain.model.json.drawn_graph.DrawnEdgeJSONFields.*;
import static org.triple_brain.model.json.drawn_graph.PointJSONFields.*;
import static org.triple_brain.module.graphviz_visualisation.JenaGraphToDrawnGraphConverter.*;

import static org.triple_brain.graphmanipulator.jena.graph.JenaGraphManipulator.*;
import static org.triple_brain.graphmanipulator.jena.graph.JenaVertexManipulator.*;
import static org.triple_brain.graphmanipulator.jena.graph.JenaEdgeManipulator.*;

/**
 * @author Vincent Blouin
 */
public class JenaGraphToDrawnGraphConverterTest {

    private JenaGraphManipulator jenaGraphManipulator;
    private JenaVertexManipulator jenaVertexManipulator;
    private JenaEdgeManipulator jenaEdgeManipulator;
    private Resource firstPerson;
    private Resource age;
    private Resource twentyHeight;
    private final Integer DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES = 10;

    @Before
    public void createJENAGraph(){
        jenaGraphManipulator = JenaGraphManipulator.jenaGraphManipulatorWithDefaultUser();
        jenaVertexManipulator = jenaVertexManipulatorWithJenaGraphManipulator(jenaGraphManipulator);
        jenaEdgeManipulator = jenaEdgeManipulatorWithJenaGraphManipulator(jenaGraphManipulator);

        firstPerson = jenaGraphManipulator.defaultUser().absoluteCentralVertex();

        Statement statement = jenaVertexManipulator.addVertexAndRelation(firstPerson.getLocalName());
        jenaEdgeManipulator.updateLabel(statement.getPredicate().getLocalName(), "Age");
        jenaVertexManipulator.updateLabel(statement.getObject().asResource().getLocalName(), "28");

        age = jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES).getResource(statement.getPredicate().getURI());
        twentyHeight = jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES).getResource(statement.getObject().asResource().getURI());
    }

    private void resetToEmptyGraph(){
        jenaGraphManipulator = jenaGraphManipulatorWithDefaultUser();
        jenaVertexManipulator = jenaVertexManipulatorWithJenaGraphManipulator(jenaGraphManipulator);
        jenaEdgeManipulator = jenaEdgeManipulatorWithJenaGraphManipulator(jenaGraphManipulator);
    }

    @Test
    public void can_convert_JENA_graph_to_JSON_drawn_graph() throws Exception {

        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));
        assertThat(drawnGraph, is(not(nullValue())));

        assertThat(drawnGraph.getJSONArray(EDGES).length(), is(2));
        assertThat(drawnGraph.getJSONArray(VERTICES).length(), is(3));

        Integer boundingBoxWidth = Integer.valueOf(drawnGraph.getString(BOUNDING_BOX_WIDTH));
        assertThat(boundingBoxWidth, is(greaterThan(0)));
        Integer boundingBoxHeight = Integer.valueOf(drawnGraph.getString(BOUNDING_BOX_HEIGHT));
        assertThat(boundingBoxHeight, is(greaterThan(0)));
    }

    @Test
    public void numbers_in_graph_have_the_right_class_type() throws Exception {
        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));
        JSONObject edge = drawnGraph.getJSONArray(EDGES).getJSONObject(0);
        JSONObject arrowLineBezierPoint = edge.getJSONArray(ARROW_LINE_BEZIER_POINTS).getJSONObject(0);
        assertThat(arrowLineBezierPoint.get(X).getClass().getName(), is("java.lang.Double"));
        assertThat(arrowLineBezierPoint.get(Y).getClass().getName(), is("java.lang.Double"));
    }

    @Test
    public void with_a_single_vertex_can_convert_JENA_graph_to_JSON_drawn_graph() throws Exception {
        resetToEmptyGraph();
        assertThat(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES).listSubjects().toList().size(), is(3));

        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));

        assertThat(drawnGraph.getJSONArray(VERTICES).length(), is(2));
        assertThat(drawnGraph.getJSONArray(EDGES).length(), is(1));
    }

    @Test
    public void with_circular_graph_can_convert_JENA_graph_to_JSON_drawn_graph() throws Exception {

        //creating graph roger_lamothe --Age-> 28 --is favorite number of-> roger_lamothe
        Statement statement = jenaEdgeManipulator.addRelationBetweenVertices(twentyHeight.getLocalName(), firstPerson.getLocalName());
        jenaEdgeManipulator.updateLabel(statement.getObject().asResource().getLocalName(), "is favorite number of");

        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));
        assertThat(drawnGraph, is(not(nullValue())));

        assertThat(drawnGraph.getJSONArray(EDGES).length(), is(3));
        assertThat(drawnGraph.getJSONArray(VERTICES).length(), is(3));
    }

    @Test
    public void all_graph_elements_are_within_the_bounding_box_of_the_graph() throws Exception{

        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));

        Integer boundingBoxWidth = Integer.valueOf(drawnGraph.getString(BOUNDING_BOX_WIDTH));
        Integer boundingBoxHeight = Integer.valueOf(drawnGraph.getString(BOUNDING_BOX_HEIGHT));

        //all vertices position's should be within the graph bounding box
        for (int i = 0; i < drawnGraph.getJSONArray(VERTICES).length(); i++) {
            JSONObject vertex = drawnGraph.getJSONArray(VERTICES).getJSONObject(i);
            JSONObject position = vertex.getJSONObject(POSITION);
            Double xPosition = Double.valueOf(position.getString(X));
            Double yPosition = Double.valueOf(position.getString(Y));
            assertThat(Double.valueOf(boundingBoxWidth), is(greaterThan(xPosition)));
            assertThat(Double.valueOf(boundingBoxHeight), is(greaterThan(yPosition)));
        }
        //all edges position's should be within the graph bounding box
        for (int i = 0; i < drawnGraph.getJSONArray(EDGES).length(); i++) {
            JSONObject edge = drawnGraph.getJSONArray(EDGES).getJSONObject(i);

            JSONObject label_position = edge.getJSONObject(LABEL_POSITION);
            Double xPosition = Double.valueOf(label_position.getString(X));
            Double yPosition = Double.valueOf(label_position.getString(Y));
            assertThat(Double.valueOf(boundingBoxWidth), is(greaterThan(xPosition)));
            assertThat(Double.valueOf(boundingBoxHeight), is(greaterThan(yPosition)));


            JSONObject arrowHeadSummit1 = edge.getJSONObject(ARROW_HEAD_SUMMIT_1);
            Double x1Position = Double.valueOf(arrowHeadSummit1.getString(X));
            Double y1Position = Double.valueOf(arrowHeadSummit1.getString(Y));
            assertThat(Double.valueOf(boundingBoxWidth), is(greaterThan(x1Position)));
            assertThat(Double.valueOf(boundingBoxHeight), is(greaterThan(y1Position)));


            JSONObject arrowHeadSummit2 = edge.getJSONObject(ARROW_HEAD_SUMMIT_2);
            Double x2Position = Double.valueOf(arrowHeadSummit2.getString(X));
            Double y2Position = Double.valueOf(arrowHeadSummit2.getString(Y));
            assertThat(Double.valueOf(boundingBoxWidth), is(greaterThan(x2Position)));
            assertThat(Double.valueOf(boundingBoxHeight), is(greaterThan(y2Position)));


            JSONObject arrowHeadSummit3 = edge.getJSONObject(ARROW_HEAD_SUMMIT_3);
            Double x3Position = Double.valueOf(arrowHeadSummit3.getString(X));
            Double y3Position = Double.valueOf(arrowHeadSummit3.getString(Y));
            assertThat(Double.valueOf(boundingBoxWidth), is(greaterThan(x3Position)));
            assertThat(Double.valueOf(boundingBoxHeight), is(greaterThan(y3Position)));

            for (int j = 0; j < edge.getJSONArray(ARROW_LINE_BEZIER_POINTS).length(); j++) {
                JSONObject arrowLineBezierPoint = edge.getJSONArray(ARROW_LINE_BEZIER_POINTS).getJSONObject(j);
                Double xArrowLineBezierPoint = Double.valueOf(arrowLineBezierPoint.getString(X));
                Double yArrowLineBezierPoint = Double.valueOf(arrowLineBezierPoint.getString(Y));
                assertThat(Double.valueOf(boundingBoxWidth), is(greaterThan(xArrowLineBezierPoint)));
                assertThat(Double.valueOf(boundingBoxHeight), is(greaterThan(yArrowLineBezierPoint)));
            }
        }
    }

    @Test
    public void write_default_label_when_label_is_empty() throws Exception {

        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));

        assertTrue(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), "Age"));
        assertTrue(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), "28"));
        assertFalse(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), EMPTY_EDGE_LABEL));
        assertFalse(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), EMPTY_VERTEX_LABEL));

        jenaEdgeManipulator.updateLabel(age.getLocalName(), "");
        jenaVertexManipulator.updateLabel(twentyHeight.getLocalName(), "");
        drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));

        assertFalse(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), "Age"));
        assertFalse(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), "28"));
        assertTrue(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), EMPTY_EDGE_LABEL));
        assertTrue(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), EMPTY_VERTEX_LABEL));
    }

    @Test
    public void json_edge_contain_source_and_destination_id() throws Exception {
        JSONObject drawnGraph = JenaGraphToDrawnGraphConverter.graphVizDrawing(jenaGraphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES));
        JSONObject ageEdge = edgeWithLabel(drawnGraph.getJSONArray(EDGES), "Age");
        assertThat(ageEdge.getString(SOURCE_VERTEX_ID),is(firstPerson.getLocalName()));
        assertThat(ageEdge.getString(DESTINATION_VERTEX_ID),is(twentyHeight.getLocalName()));
    }

    @Test
    public void vertices_have_their_minimum_number_of_edges_from_center_vertex()throws Exception{
        Model subGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES, firstPerson.getLocalName());
        JSONObject drawnGraph = graphVizDrawing(subGraph);
        JSONObject jsonRogerLamothe = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        JSONObject jsonTwentyHeight = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "28");
        assertThat(jsonRogerLamothe.getInt(MIN_NUMBER_OF_EDGES_FROM_CENTER_VERTEX), is(0));
        assertThat(jsonTwentyHeight.getInt(MIN_NUMBER_OF_EDGES_FROM_CENTER_VERTEX), is(1));
    }

    @Test
    public void vertices_at_the_maximum_depth_of_center_vertices_that_have_more_sub_vertices_have_a_special_property() throws Exception{
        Statement statement = jenaVertexManipulator.addVertexAndRelation(firstPerson.getLocalName());
        Resource nicknameAttribute = statement.getPredicate();
        Resource nickname = statement.getObject().asResource();
        jenaEdgeManipulator.updateLabel(nicknameAttribute.getLocalName(), "nickname");
        jenaVertexManipulator.updateLabel(nickname.getLocalName(), "Bob");

        Model jenaGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(2, twentyHeight.getLocalName());
        JSONObject drawnGraph = graphVizDrawing(jenaGraph);
        JSONObject firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertFalse(firstPersonVertex.has(IS_FRONTIER_VERTEX_WITH_HIDDEN_VERTICES));

        jenaGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(1, twentyHeight.getLocalName());
        drawnGraph = graphVizDrawing(jenaGraph);
        firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertTrue(firstPersonVertex.has(IS_FRONTIER_VERTEX_WITH_HIDDEN_VERTICES));
    }

    @Test
    public void frontier_vertices_with_hidden_vertices_hold_their_number_of_hidden_vertices()throws Exception{
        Statement statement = jenaVertexManipulator.addVertexAndRelation(firstPerson.getLocalName());
        Resource nicknameAttribute = statement.getPredicate();
        Resource nickname = statement.getObject().asResource();
        jenaEdgeManipulator.updateLabel(nicknameAttribute.getLocalName(), "nickname");
        jenaVertexManipulator.updateLabel(nickname.getLocalName(), "Bob");

        Model jenaGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(2, twentyHeight.getLocalName());
        JSONObject drawnGraph = graphVizDrawing(jenaGraph);
        JSONObject firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertFalse(firstPersonVertex.has(NUMBER_OF_HIDDEN_CONNECTED_VERTICES));

        jenaGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(1, twentyHeight.getLocalName());
        drawnGraph = graphVizDrawing(jenaGraph);
        firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertThat(firstPersonVertex.getInt(NUMBER_OF_HIDDEN_CONNECTED_VERTICES), is(2));
    }

    @Test
    public void frontier_vertices_with_hidden_vertices_hold_names_of_their_hidden_properties()throws Exception{
        Statement statement = jenaVertexManipulator.addVertexAndRelation(firstPerson.getLocalName());
        Resource nicknameAttribute = statement.getPredicate();
        Resource nickname = statement.getObject().asResource();
        jenaEdgeManipulator.updateLabel(nicknameAttribute.getLocalName(), "nickname");
        jenaVertexManipulator.updateLabel(nickname.getLocalName(), "Bob");

        Model jenaGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(2, twentyHeight.getLocalName());
        JSONObject drawnGraph = graphVizDrawing(jenaGraph);
        JSONObject firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertFalse(firstPersonVertex.has(NAME_OF_HIDDEN_PROPERTIES));

        jenaGraph = jenaGraphManipulator.graphWithDepthAndCenterVertexId(1, twentyHeight.getLocalName());
        drawnGraph = graphVizDrawing(jenaGraph);
        firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertThat(firstPersonVertex.getJSONArray(NAME_OF_HIDDEN_PROPERTIES).length(), is(2));
        assertTrue(JSONArrayContainsString(firstPersonVertex.getJSONArray(NAME_OF_HIDDEN_PROPERTIES), "name"));
        assertTrue(JSONArrayContainsString(firstPersonVertex.getJSONArray(NAME_OF_HIDDEN_PROPERTIES), "nickname"));
    }

    private boolean JSONArrayContainsString(JSONArray jsonArray, String stringToTest) throws JSONException{
        for(int i = 0 ; i < jsonArray.length(); i++){
            if(jsonArray.getString(i).equals(stringToTest)){
                return true;
            }
        }
        return false;
    }

    private JSONObject vertexWithLabel(JSONArray vertices, String label) throws Exception{
        for(int i = 0 ; i < vertices.length(); i++){
            if(vertices.getJSONObject(i).getString(VertexJSONFields.LABEL).equals(label)){
                return vertices.getJSONObject(i);
            }
        }
        return null;
    }

    private JSONObject edgeWithLabel(JSONArray edges, String label) throws Exception{
        for(int i = 0 ; i < edges.length(); i++){
            if(edges.getJSONObject(i).getString(EdgeJSONFields.LABEL).equals(label)){
                return edges.getJSONObject(i);
            }
        }
        return null;
    }

    private boolean containsVertexWithLabel(JSONArray vertices, String label) throws Exception{
        for(int i = 0 ; i < vertices.length(); i++){
            if(vertices.getJSONObject(i).getString(VertexJSONFields.LABEL).equals(label)){
                return true;
            }
        }
        return false;
    }

    private boolean containsEdgeWithLabel(JSONArray edges, String label) throws Exception{
        for(int i = 0 ; i < edges.length(); i++){
            if(edges.getJSONObject(i).getString(VertexJSONFields.LABEL).equals(label)){
                return true;
            }
        }
        return false;
    }

}
