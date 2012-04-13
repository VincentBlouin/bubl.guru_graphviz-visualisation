package org.triple_brain.module.graphviz_visualisation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.UUID;


/*
Copyright Mozilla Public License 1.1
*/
public class GraphvizService {

    public static final String GRAPH_VIZ_TEMP_FILES_RELATIVE_FOLDER_PATH = "./";

    public GraphvizService(){
    }

    public String convertToDotDrawnGraph(String dotGraph) {
        byte bytes[] = graphFromGraphViz(GRAPH_VIZ_TEMP_FILES_RELATIVE_FOLDER_PATH, dotGraph, "xdot");
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
            img = new File(pathToFile + UUID.randomUUID().toString());
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
            temp = new File(pathToFile + UUID.randomUUID().toString());
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
}
