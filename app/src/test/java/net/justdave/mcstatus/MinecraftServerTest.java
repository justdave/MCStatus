package net.justdave.mcstatus;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.net.URISyntaxException;

public class MinecraftServerTest {

    @Test
    public void testMinecraftToHtml_WebView() throws URISyntaxException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        // Color test
        assertEquals("<span></span><span style=\"color: #00aaaa; \">Text</span>", 
            server.minecraftToHtml("§3Text", true));
        
        // Bold and Italic test
        assertEquals("<span></span><span style=\"font-weight: bold; font-style: italic; \">Text</span>", 
            server.minecraftToHtml("§l§oText", true));
            
        // Reset test
        assertEquals("<span></span><span style=\"color: #00aaaa; \">Part1</span><span style=\"\">Part2</span>", 
            server.minecraftToHtml("§3Part1§rPart2", true));
    }

    @Test
    public void testMinecraftToHtml_TextView() throws URISyntaxException {
        MinecraftServer server = new MinecraftServer("Test", "localhost");
        
        // Color test
        assertEquals("</font></b></i></u><font color=\"#00aaaa\">Text</font></b></i></u>", 
            server.minecraftToHtml("§3Text", false));
            
        // Multiple styles
        assertEquals("</font></b></i></u><font><b><i><u>Text</font></b></i></u>", 
            server.minecraftToHtml("§l§o§nText", false));
    }
}
