package net.justdave.mcstatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MinecraftServerTest {

    @Test
    public void testMinecraftToHtml_WebView() throws URISyntaxException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        // Color test
        assertEquals("</font></b></i></u></strike><font color=\"#00aaaa\">Text</font></b></i></u></strike>", 
            server.minecraftToHtml("§3Text"));
        
        // Bold and Italic test
        assertEquals("</font></b></i></u></strike><font><b><i>Text</font></b></i></u></strike>", 
            server.minecraftToHtml("§l§oText"));
            
        // Reset test
        assertEquals("</font></b></i></u></strike><font color=\"#00aaaa\">Part1</font></b></i></u></strike><font>Part2</font></b></i></u></strike>", 
            server.minecraftToHtml("§3Part1§rPart2"));

        // New line and space test
        assertEquals("</font></b></i></u></strike><font>Line1</font></b></i></u></strike><br></font></b></i></u></strike><font>&nbsp;Line2</font></b></i></u></strike>",
            server.minecraftToHtml("Line1\n Line2"));
            
        // Word wrap test
        assertEquals("</font></b></i></u></strike><font>Word1 Word2</font></b></i></u></strike>",
            server.minecraftToHtml("Word1 Word2"));
    }

    @Test
    public void testMinecraftToHtml_TextView() throws URISyntaxException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        // Color test
        assertEquals("</font></b></i></u></strike><font color=\"#00aaaa\">Text</font></b></i></u></strike>", 
            server.minecraftToHtml("§3Text"));
            
        // Multiple styles
        assertEquals("</font></b></i></u></strike><font><b><i><u>Text</font></b></i></u></strike>", 
            server.minecraftToHtml("§l§o§nText"));
    }
    @Test
    public void testDescription_ComplexJSON() throws URISyntaxException, JSONException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        JSONObject json = new JSONObject();
        JSONObject description = new JSONObject();
        description.put("text", "Root");
        description.put("color", "red");
        
        JSONArray extra = new JSONArray();
        JSONObject extra1 = new JSONObject();
        extra1.put("text", "Extra1");
        extra1.put("bold", true);
        extra.put(extra1);
        
        description.put("extra", extra);
        json.put("description", description);
        
        server.setServerJSON(json);
        
        String html = server.description();
        // Check for red color on Root
        assertTrue(html.contains("color=\"#ff5555\""));
        assertTrue(html.contains("Root"));
        // Check for bold on Extra1 and inheritance of color red
        assertTrue(html.contains("<b>"));
        assertTrue(html.contains("Extra1"));
    }
}
