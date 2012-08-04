package org.triple_brain.module.graphviz_visualisation;

import com.google.inject.Guice;
import graph.JenaSQLTestModule;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.triple_brain.graphmanipulator.jena.graph.JenaGraphManipulator;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Graph;
import org.triple_brain.module.model.graph.Vertex;
import org.triple_brain.module.model.json.graph.EdgeJsonFields;
import org.triple_brain.module.model.json.graph.VertexJsonFields;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.triple_brain.graphmanipulator.jena.JenaConnection.closeConnection;
import static org.triple_brain.module.model.json.drawn_graph.DrawnEdgeJSONFields.*;
import static org.triple_brain.module.model.json.drawn_graph.DrawnGraphJSONFields.*;
import static org.triple_brain.module.model.json.drawn_graph.DrawnVertexJSONFields.*;
import static org.triple_brain.module.model.json.drawn_graph.PointJSONFields.X;
import static org.triple_brain.module.model.json.drawn_graph.PointJSONFields.Y;

/**
 * Copyright Mozilla Public License 1.1
 */
public class GraphToDrawnGraphConverterTest {

    private JenaGraphManipulator graphManipulator;
    private Vertex me;
    private Edge age;
    private Vertex twentyHeight;
    private final Integer DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES = 10;

    @BeforeClass
    public static void beforeClass() throws Exception{
        Guice.createInjector(new JenaSQLTestModule());
    }

    private User user = User.withUsernameAndEmail(
            "roger_lamothe",
            "roger.lamothe@example.org"
    );

    @Before
    public void before() throws Exception{
        makeGraphHaveOnlyDefaultCenterVertex();
        createVertexFirstPersonThatHaveEdgeAgePointingToVertexTwentyHeight();
    }

    @AfterClass
    public static void after()throws Exception{
        closeConnection();
    }

    private void makeGraphHaveOnlyDefaultCenterVertex() throws Exception{
        graphManipulator = JenaGraphManipulator.withUser(user);
        graphManipulator.model().removeAll();
        JenaGraphManipulator.createUserGraph(user);
    }

    private void createVertexFirstPersonThatHaveEdgeAgePointingToVertexTwentyHeight(){
        me = graphManipulator.defaultVertex();
        age = me.addVertexAndRelation();
        age.label("Age");
        twentyHeight = age.destinationVertex();
        twentyHeight.label("28");
    }

    @Test
    public void can_convert_graph_to_JSON_drawn_graph() throws Exception {
        JSONObject drawnGraph = convertWholeGraph();

        assertThat(drawnGraph, is(not(nullValue())));

        assertThat(drawnGraph.getJSONArray(EDGES).length(), is(1));
        assertThat(drawnGraph.getJSONArray(VERTICES).length(), is(2));

        Integer boundingBoxWidth = Integer.valueOf(drawnGraph.getString(BOUNDING_BOX_WIDTH));
        assertThat(boundingBoxWidth, is(greaterThan(0)));
        Integer boundingBoxHeight = Integer.valueOf(drawnGraph.getString(BOUNDING_BOX_HEIGHT));
        assertThat(boundingBoxHeight, is(greaterThan(0)));
    }

    @Test
    public void numbers_in_graph_have_the_right_class_type() throws Exception {
        JSONObject drawnGraph = convertWholeGraph();
        JSONObject edge = drawnGraph.getJSONArray(EDGES).getJSONObject(0);
        JSONObject arrowLineBezierPoint = edge.getJSONArray(ARROW_LINE_BEZIER_POINTS).getJSONObject(0);
        assertThat(arrowLineBezierPoint.get(X).getClass().getName(), is("java.lang.Double"));
        assertThat(arrowLineBezierPoint.get(Y).getClass().getName(), is("java.lang.Double"));
    }

    @Test
    public void with_a_single_vertex_can_convert_graph_to_json_drawn_graph() throws Exception {
        makeGraphHaveOnlyDefaultCenterVertex();
        assertThat(wholeGraph().numberOfVertices(), is(1));
        assertThat(wholeGraph().numberOfEdges(), is(0));

        JSONObject drawnGraph = convertWholeGraph();

        assertThat(drawnGraph.getJSONArray(VERTICES).length(), is(1));
        assertThat(drawnGraph.getJSONArray(EDGES).length(), is(0));
    }

    @Test
    public void with_circular_graph_can_convert_JENA_graph_to_JSON_drawn_graph() throws Exception {

        Edge edge = twentyHeight.addRelationToVertex(me);
        edge.label("is favorite number of");

        JSONObject drawnGraph = convertWholeGraph();
        assertThat(drawnGraph, is(not(nullValue())));

        assertThat(drawnGraph.getJSONArray(EDGES).length(), is(2));
        assertThat(drawnGraph.getJSONArray(VERTICES).length(), is(2));
    }

    @Test
    public void all_graph_elements_are_within_the_bounding_box_of_the_graph() throws Exception{

        JSONObject drawnGraph = convertWholeGraph();

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

        JSONObject drawnGraph = convertWholeGraph();

        assertTrue(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), "Age"));
        assertTrue(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), "28"));
        assertFalse(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), Edge.EMPTY_LABEL));
        assertFalse(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), Vertex.EMPTY_LABEL));

        age.label("");
        twentyHeight.label("");
        drawnGraph = convertWholeGraph();

        assertFalse(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), "Age"));
        assertFalse(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), "28"));
        assertTrue(containsEdgeWithLabel(drawnGraph.getJSONArray(EDGES), Edge.EMPTY_LABEL));
        assertTrue(containsVertexWithLabel(drawnGraph.getJSONArray(VERTICES), Vertex.EMPTY_LABEL));
    }

    @Test
    public void json_edge_contain_source_and_destination_id() throws Exception {
        JSONObject drawnGraph = convertWholeGraph();
        JSONObject ageEdge = edgeWithLabel(drawnGraph.getJSONArray(EDGES), "Age");
        assertThat(ageEdge.getString(SOURCE_VERTEX_ID),is(me.id()));
        assertThat(ageEdge.getString(DESTINATION_VERTEX_ID),is(twentyHeight.id()));
    }

    @Test
    public void vertices_at_the_maximum_depth_of_center_vertices_that_have_more_sub_vertices_have_a_special_property() throws Exception{
        addNickNameBobToMe();

        Graph subGraph = graphManipulator.graphWithDepthAndCenterVertexId(
                2, twentyHeight.id()
        );
        JSONObject drawnGraph = convertGraph(subGraph);
        JSONObject firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertFalse(firstPersonVertex.has(IS_FRONTIER_VERTEX_WITH_HIDDEN_VERTICES));

        subGraph = graphManipulator.graphWithDepthAndCenterVertexId(
                1, twentyHeight.id()
        );
        drawnGraph = convertGraph(subGraph);
        firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertTrue(firstPersonVertex.has(IS_FRONTIER_VERTEX_WITH_HIDDEN_VERTICES));
    }

    @Test
    public void frontier_vertices_with_hidden_vertices_hold_their_number_of_hidden_vertices()throws Exception{
        addNickNameBobToMe();

        Graph subGraph = graphManipulator.graphWithDepthAndCenterVertexId(
                2,
                twentyHeight.id()
        );
        JSONObject drawnGraph = convertGraph(subGraph);
        JSONObject meVertex = vertexWithLabel(
                drawnGraph.getJSONArray(VERTICES),
                "me"
        );
        assertFalse(
                meVertex.has(NUMBER_OF_HIDDEN_CONNECTED_VERTICES)
        );

        subGraph = graphManipulator.graphWithDepthAndCenterVertexId(
                1, twentyHeight.id());
        drawnGraph = convertGraph(subGraph);
        meVertex = vertexWithLabel(
                drawnGraph.getJSONArray(VERTICES), "me"
        );
        assertThat(
                meVertex.getInt(NUMBER_OF_HIDDEN_CONNECTED_VERTICES),
                is(1)
        );
    }

    @Test
    public void frontier_vertices_with_hidden_vertices_hold_names_of_their_hidden_properties()throws Exception{
        addNickNameBobToMe();
        Graph subGraph =
                graphManipulator.graphWithDepthAndCenterVertexId(
                        2, twentyHeight.id()
                );
        JSONObject drawnGraph = convertGraph(subGraph);
        JSONObject firstPersonVertex = vertexWithLabel(
                drawnGraph.getJSONArray(VERTICES), "me"
        );
        assertFalse(firstPersonVertex.has(NAME_OF_HIDDEN_PROPERTIES));

        subGraph = graphManipulator.graphWithDepthAndCenterVertexId(
                1, twentyHeight.id());
        drawnGraph = convertGraph(subGraph);
        firstPersonVertex = vertexWithLabel(drawnGraph.getJSONArray(VERTICES), "me");
        assertThat(
                firstPersonVertex.getJSONArray(NAME_OF_HIDDEN_PROPERTIES).length(),
                is(1)
        );
        assertTrue(
                JSONArrayContainsString(firstPersonVertex
                        .getJSONArray(NAME_OF_HIDDEN_PROPERTIES),
                        "nickname")
        );
    }

    private Edge addNickNameBobToMe(){
        Edge nickname = me.addVertexAndRelation();
        nickname.label("nickname");
        Vertex bob = nickname.destinationVertex();
        bob.label("Bob");
        return nickname;
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
            if(vertices.getJSONObject(i).getString(VertexJsonFields.LABEL).equals(label)){
                return vertices.getJSONObject(i);
            }
        }
        return null;
    }

    private JSONObject edgeWithLabel(JSONArray edges, String label) throws Exception{
        for(int i = 0 ; i < edges.length(); i++){
            if(edges.getJSONObject(i).getString(EdgeJsonFields.LABEL).equals(label)){
                return edges.getJSONObject(i);
            }
        }
        return null;
    }

    private boolean containsVertexWithLabel(JSONArray vertices, String label) throws Exception{
        for(int i = 0 ; i < vertices.length(); i++){
            if(vertices.getJSONObject(i).getString(VertexJsonFields.LABEL).equals(label)){
                return true;
            }
        }
        return false;
    }

    private boolean containsEdgeWithLabel(JSONArray edges, String label) throws Exception{
        for(int i = 0 ; i < edges.length(); i++){
            if(edges.getJSONObject(i).getString(VertexJsonFields.LABEL).equals(label)){
                return true;
            }
        }
        return false;
    }

    private Graph wholeGraph(){
        return graphManipulator.graphWithDefaultVertexAndDepth(DEPTH_OF_SUB_VERTICES_COVERING_ALL_GRAPH_VERTICES);
    }


    private JSONObject convertWholeGraph()throws JSONException{
        return GraphToDrawnGraphConverter.withGraph(wholeGraph()).convert();
    }

    private JSONObject convertGraph(Graph graph)throws JSONException{
        return GraphToDrawnGraphConverter.withGraph(graph).convert();
    }

}
