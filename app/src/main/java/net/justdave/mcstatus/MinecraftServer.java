/*
 * While not entirely a direct port, this file is heavily based on 
 */

package net.justdave.mcstatus;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MinecraftServer {
	private static final String TAG = MinecraftServer.class.getSimpleName();
	private static final int THUMBNAIL_SIZE = 64;

	private final String serverName;
	private final String serverAddress;
	private int queryPort = 25565; // the default minecraft query port
	private JSONObject serverJSON = new JSONObject();

	public MinecraftServer(String name, String address) throws URISyntaxException {
		serverName = name;
		Log.i(TAG, "new MinecraftServer(".concat(address).concat(")"));
		URI uri = new URI("my://" + address);
		serverAddress = uri.getHost();
		if (uri.getPort() > 0) {
			queryPort = uri.getPort();
		}
		setDescription("Loading...");
	}

	private static int getPowerOfTwoForSampleRatio(double ratio) {
		int k = Integer.highestOneBit((int) Math.floor(ratio));
		if (k == 0)
			return 1;
		else
			return k;
	}

	public void setDescription(String msg) {
		try {
			serverJSON.put("description", msg);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public String serverAddress() {
		StringBuilder result = new StringBuilder();
		result.append(serverAddress);
		if (queryPort != 25565) { // if it's not the default minecraft port
			result.append(":");
			result.append(queryPort);
		}
		return result.toString();
	}

	public String serverName() {
		return serverName;
	}

	public int maxPlayers() {
		JSONObject players = serverJSON.optJSONObject("players");
		if (players == null) {
			return 0;
		}
		return players.optInt("max");
	}

	public int onlinePlayers() {
		JSONObject players = serverJSON.optJSONObject("players");
		if (players == null) {
			return 0;
		}
		return players.optInt("online");
	}

	public ArrayList<String> playerList() {
		ArrayList<String> result = new ArrayList<>();
		JSONObject players = serverJSON.optJSONObject("players");
		if (players == null) {
			return result;
		}
		JSONArray sample = players.optJSONArray("sample");
		if (sample == null) {
			return result;
		}
		int pos = 0;
		while (pos < sample.length()) {
			JSONObject entry = sample.optJSONObject(pos++);
			String username = entry.optString("name");
			result.add(minecraftToHtml(username));
		}
		Log.i(TAG, "playerList() returning ".concat(result.toString()));
		return result;
	}

	public String serverVersion() {
		JSONObject version = serverJSON.optJSONObject("version");
		if (version == null) {
			return "";
		}
		return minecraftToHtml(version.optString("name"));
	}

	private static class Style {
		String color = "";
		boolean bold = false;
		boolean italic = false;
		boolean strikethrough = false;
		boolean underlined = false;

		Style copy() {
			Style copy = new Style();
			copy.color = this.color;
			copy.bold = this.bold;
			copy.italic = this.italic;
			copy.strikethrough = this.strikethrough;
			copy.underlined = this.underlined;
			return copy;
		}

		static String colorToHex(String color) {
			if (color == null || color.isEmpty()) return "";
			if (color.startsWith("#")) return color;
			switch (color.toLowerCase()) {
				case "black": return "#000000";
				case "dark_blue": return "#0000aa";
				case "dark_green": return "#00aa00";
				case "dark_aqua": return "#00aaaa";
				case "dark_red": return "#aa0000";
				case "dark_purple": return "#aa00aa";
				case "gold": return "#ffaa00";
				case "gray": return "#aaaaaa";
				case "dark_gray": return "#555555";
				case "blue": return "#5555ff";
				case "green": return "#55ff55";
				case "aqua": return "#55ffff";
				case "red": return "#ff5555";
				case "light_purple": return "#ff55ff";
				case "yellow": return "#ffff55";
				case "white": return "#ffffff";
				default: return "";
			}
		}
	}

	public String description() {
		Object description = serverJSON.opt("description");
		if (description != null) {
			return parseComponent(description, new Style());
		}
		return "";
	}

	private String parseComponent(Object component, Style inheritedStyle) {
		StringBuilder result = new StringBuilder();
		Style currentStyle = inheritedStyle.copy();

		if (component instanceof String) {
			result.append(minecraftToHtml((String) component, currentStyle));
		} else if (component instanceof JSONObject) {
			JSONObject obj = (JSONObject) component;
			if (obj.has("color")) currentStyle.color = Style.colorToHex(obj.optString("color"));
			if (obj.has("bold")) currentStyle.bold = obj.optBoolean("bold");
			if (obj.has("italic")) currentStyle.italic = obj.optBoolean("italic");
			if (obj.has("underlined")) currentStyle.underlined = obj.optBoolean("underlined");
			if (obj.has("strikethrough")) currentStyle.strikethrough = obj.optBoolean("strikethrough");

			String text = obj.optString("text");
			if (!text.isEmpty()) {
				result.append(minecraftToHtml(text, currentStyle));
			}

			JSONArray extra = obj.optJSONArray("extra");
			if (extra != null) {
				for (int i = 0; i < extra.length(); i++) {
					Object extraComponent = extra.opt(i);
					if (extraComponent != null) {
						result.append(parseComponent(extraComponent, currentStyle));
					}
				}
			}
		} else if (component instanceof JSONArray) {
			JSONArray array = (JSONArray) component;
			for (int i = 0; i < array.length(); i++) {
				Object entry = array.opt(i);
				if (entry != null) {
					result.append(parseComponent(entry, currentStyle));
				}
			}
		}

		return result.toString();
	}

	String minecraftToHtml(String text, Style baseStyle) {
		StringBuilder result = new StringBuilder();
		int curChar = 0;
		Style currentStyle = baseStyle.copy();
		boolean stylesChanged = true;
		boolean isAtStartOfLine = true;

		while (curChar < text.length()) {
			char theChar = text.charAt(curChar++);
			if (theChar == '§' && curChar < text.length()) {
				char code = text.charAt(curChar++);
				switch (code) {
					case '0': currentStyle.color = "#000000"; break;
					case '1': currentStyle.color = "#0000aa"; break;
					case '2': currentStyle.color = "#00aa00"; break;
					case '3': currentStyle.color = "#00aaaa"; break;
					case '4': currentStyle.color = "#aa0000"; break;
					case '5': currentStyle.color = "#aa00aa"; break;
					case '6': currentStyle.color = "#ffaa00"; break;
					case '7': currentStyle.color = "#aaaaaa"; break;
					case '8': currentStyle.color = "#555555"; break;
					case '9': currentStyle.color = "#5555ff"; break;
					case 'a': currentStyle.color = "#55ff55"; break;
					case 'b': currentStyle.color = "#55ffff"; break;
					case 'c': currentStyle.color = "#ff5555"; break;
					case 'd': currentStyle.color = "#ff55ff"; break;
					case 'e': currentStyle.color = "#ffff55"; break;
					case 'f': currentStyle.color = "#ffffff"; break;
					case 'l': currentStyle.bold = true; break;
					case 'm': currentStyle.strikethrough = true; break;
					case 'n': currentStyle.underlined = true; break;
					case 'o': currentStyle.italic = true; break;
					case 'r':
						currentStyle = baseStyle.copy();
						break;
				}
				stylesChanged = true;
			} else if (theChar == '\n') {
				result.append("</font></b></i></u></strike><br>");
				stylesChanged = true;
				isAtStartOfLine = true;
			} else {
				if (stylesChanged) {
					result.append("</font></b></i></u></strike>");
					result.append("<font");
					if (!currentStyle.color.isEmpty()) result.append(" color=\"").append(currentStyle.color).append("\"");
					result.append(">");
					if (currentStyle.bold) result.append("<b>");
					if (currentStyle.italic) result.append("<i>");
					if (currentStyle.underlined) result.append("<u>");
					if (currentStyle.strikethrough) result.append("<strike>");
					stylesChanged = false;
				}
				if (theChar == ' ') {
					if (isAtStartOfLine) {
						result.append("&nbsp;");
					} else {
						result.append(' ');
					}
				} else {
					result.append(theChar);
					isAtStartOfLine = false;
				}
			}
		}
		result.append("</font></b></i></u></strike>");
		return result.toString();
	}

	String minecraftToHtml(String text) {
		return minecraftToHtml(text, new Style());
	}


	public Bitmap image() {
		String imageData = serverJSON.optString("favicon");
		if (imageData.isEmpty())
			return null;
		try {
			return getThumbnail(imageData);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void query() {
		Log.i(TAG, "query() called on ".concat(serverAddress()));
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(serverAddress, queryPort), 10000);
			socket.setSoTimeout(10000); // 10 second read timeout

			// See http://wiki.vg/Protocol (Status Ping)
			try (OutputStream out = socket.getOutputStream();
				 InputStream in = socket.getInputStream()) {
				out.write(6 + serverAddress.length()); // length of packet ID + data to follow
				out.write(0); // packet ID = 0
				out.write(4); // Protocol version
				out.write(serverAddress.length()); // varint length of server address
				out.write(serverAddress.getBytes()); // server address in UTF8
				out.write((queryPort & 0xFF00) >> 8); // port number high byte
				out.write(queryPort & 0x00FF); // port number low byte
				out.write(1); // Next state: status
				out.write(0x01); // status ping (1st byte)
				out.write(0x00); // status ping (2nd byte)

				int packetLength = readVarInt(in); // size of entire packet
				if (packetLength < 11) {
					Log.i(TAG, String.format("%s, %s: packet length too small: %d", serverName, serverAddress, packetLength));
					setDescription("Invalid response from server (server may be in the process of restarting, try again in a few seconds)");
					return;
				}
				final int packetType = in.read(); // packet type - going to ignore it
				Log.d(TAG, String.format("%s, %s: packet type: %d", serverName, serverAddress, packetType));
				int jsonLength = readVarInt(in); // size of JSON blob
				if (jsonLength < 0) {
					return;
				}
				byte[] buffer = new byte[jsonLength];
				int totalBytesRead = 0;
				while (totalBytesRead < jsonLength) {
					int bytesRead = in.read(buffer, totalBytesRead, jsonLength - totalBytesRead);
					if (bytesRead == -1) {
						throw new IOException("Unexpected end of stream");
					}
					totalBytesRead += bytesRead;
				}
				String serverData = new String(buffer, 0, totalBytesRead);
				serverJSON = new JSONObject(serverData);
				Log.i(TAG, "ServerJSON = " + serverJSON);
			}
		} catch (UnknownHostException e) {
			setDescription("Error: Lookup failed: Unknown host");
		} catch (IllegalArgumentException | IOException | JSONException e) {
			setDescription("Server is offline or Query is disabled");
			Log.e(TAG, "Exception during query: " + e.getMessage());
		}
	}

	void setServerJSON(JSONObject json) {
		this.serverJSON = json;
	}

	private int readVarInt(InputStream in) {
		int theInt = 0;

		for (int i = 0; i < 6; i++){
			int theByte;
			try {
				theByte = in.read();
				if (theByte == -1) {
					Log.w(TAG, "readVarInt: Unexpected end of stream");
					return -1;
				}
			} catch (IOException e){
				e.printStackTrace();
				Log.w(TAG, "readVarInt: Failed to retrieve data from server");
				return -1;
			}

			theInt |= (theByte & 0x7F) << (7 * i);
			if ((theByte & 0x80) != 128) {
				break;
			}
		}
		return theInt;
	}

	public Bitmap getThumbnail(String uri) throws IOException {
		// data:image/png;base64,iVBORw0KGgoAAAANSUhEUgA
		final String prefixString = "data:image/png;base64,";
		byte[] imageDataBase64;
		byte[] imageData;
		if (uri.startsWith(prefixString)) {
			imageDataBase64 = uri.substring(prefixString.length()).getBytes();
		} else {
			throw new FileNotFoundException("Not the correct URI Prefix");
		}
		imageData = Base64.decode(imageDataBase64, Base64.DEFAULT);
		BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
		onlyBoundsOptions.inJustDecodeBounds = true;
		onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// optional
		BitmapFactory.decodeByteArray(imageData, 0, imageData.length,
				onlyBoundsOptions);
		if ((onlyBoundsOptions.outWidth == -1)
				|| (onlyBoundsOptions.outHeight == -1))
			return null;

		int originalSize = Math.max(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth);

		double ratio = (originalSize > THUMBNAIL_SIZE) ? (double)(originalSize / THUMBNAIL_SIZE)
				: 1.0;

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
		bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// optional
		return BitmapFactory.decodeByteArray(imageData, 0,
				imageData.length, bitmapOptions);
	}
}
