package net.justdave.mcstatus;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import java.net.URISyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MinecraftServerTest {

    @Test
    public void testMinecraftToSpannable() throws URISyntaxException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        assertNotNull(server.minecraftToSpannable("§3Text", new MinecraftServer.Style()));
        assertTrue(server.minecraftToSpannable("§3Text", new MinecraftServer.Style()) instanceof CharSequence);

        assertNotNull(server.minecraftToSpannable("§3Part1§rPart2", new MinecraftServer.Style()));
        assertTrue(server.minecraftToSpannable("§3Part1§rPart2", new MinecraftServer.Style()) instanceof CharSequence);

        assertNotNull(server.minecraftToSpannable("Line1\n Line2", new MinecraftServer.Style()));
        assertTrue(server.minecraftToSpannable("Line1\n Line2", new MinecraftServer.Style()) instanceof CharSequence);
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
        assertNotNull(output);
        assertTrue(output instanceof CharSequence);
    }
}
