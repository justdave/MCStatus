package net.justdave.mcstatus;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MinecraftServerTest {

    @Test
    public void testMinecraftToSpannable() throws URISyntaxException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        // Basic check for text integrity
        assertEquals("Text", server.minecraftToSpannable("§3Text", new MinecraftServer.Style()).toString());
        assertEquals("Part1Part2", server.minecraftToSpannable("§3Part1§rPart2", new MinecraftServer.Style()).toString());
        assertEquals("Line1\n Line2", server.minecraftToSpannable("Line1\n Line2", new MinecraftServer.Style()).toString());
    }

    @Test
    public void testDescription_Spannable() throws URISyntaxException, JSONException {
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
        
        CharSequence output = server.description();
        assertEquals("RootExtra1", output.toString());
    }
}
