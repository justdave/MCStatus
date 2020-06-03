/*
 * While not entirely a direct port, this file is heavily based on 
 */

package net.justdave.mcstatus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

public class MinecraftServer {
	private static final String TAG = MinecraftServer.class.getSimpleName();
	private static final int THUMBNAIL_SIZE = 64;

	private String serverName = "My Minecraft Server";
	private String serverAddress = "localhost";
	private int queryPort = 25565; // the default minecraft query port
	private JSONObject serverJSON = new JSONObject();

	public MinecraftServer() { // default to localhost:25565
		setDescription("Loading...");
	}

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
		result.clear();
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
			result.add(username);
		}
		Log.i(TAG, "playerList() returning ".concat(result.toString()));
		return result;
	}

	public String serverVersion() {
		JSONObject version = serverJSON.optJSONObject("version");
		if (version == null) {
			return "";
		}
		return version.optString("name");
	}

	public String description() {
		StringBuilder result = new StringBuilder();
		String desc = serverJSON.optString("description");
		JSONObject descriptionObj = serverJSON.optJSONObject("description");
		if (descriptionObj != null) {
			desc = descriptionObj.optString("text");
        }
		result.append("<body style='background-color: transparent; color: white; margin: 0; padding: 0;'><span>");
		int curChar = 0;
		String color = "";
		String decoration = "";
		while (curChar < desc.length()) {
			char theChar = desc.charAt(curChar++);
			if (theChar == '§') {
				char code = desc.charAt(curChar++);
				switch (code) {
				case '0':
					color = "#000000";
					decoration = "";
					break;
				case '1':
					color = "#0000aa";
					decoration = "";
					break;
				case '2':
					color = "#00aa00";
					decoration = "";
					break;
				case '3':
					color = "#00aaaa";
					decoration = "";
					break;
				case '4':
					color = "#aa0000";
					decoration = "";
					break;
				case '5':
					color = "#aa00aa";
					decoration = "";
					break;
				case '6':
					color = "#ffaa00";
					decoration = "";
					break;
				case '7':
					color = "#aaaaaa";
					decoration = "";
					break;
				case '8':
					color = "#555555";
					decoration = "";
					break;
				case '9':
					color = "#5555ff";
					decoration = "";
					break;
				case 'a':
					color = "#55ff55";
					decoration = "";
					break;
				case 'b':
					color = "#55ffff";
					decoration = "";
					break;
				case 'c':
					color = "#ff5555";
					decoration = "";
					break;
				case 'd':
					color = "#ff55ff";
					decoration = "";
					break;
				case 'e':
					color = "#ffff55";
					decoration = "";
					break;
				case 'f':
					color = "#ffffff";
					decoration = "";
					break;
				case 'k':
					color = "";
					break;
				case 'l':
					decoration = "bold";
					break;
				case 'm':
					decoration = "strikethrough";
					break;
				case 'n':
					decoration = "underscore";
					break;
				case 'o':
					decoration = "italic";
					break;
				case 'r':
					decoration = "";
					color = "";
				}
				result.append("</span><span style='");
                if (color.length() > 0) {
                	result.append("color: ".concat(color).concat("; "));
                }
                if (decoration.length() > 0) {
                	result.append("text-decoration: ".concat(decoration).concat("; "));
                }
                result.append("'>");
			} else {
				result.append(theChar);
			}
		}
		result.append("</span></body>");
		return result.toString();
	}

	public Bitmap image() {
		String imageData = serverJSON.optString("favicon");
		if (imageData.isEmpty())
			return null;
		try {
			return getThumbnail(imageData);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void query() {
		Log.i(TAG, "query() called on ".concat(serverAddress()));
		Socket socket;
		try {
			socket = new Socket(serverAddress, queryPort);
			socket.setSoTimeout(10000); // 10 second read timeout
		} catch (IllegalArgumentException e) {
			setDescription("Error: " + e.getLocalizedMessage());
			return;
		} catch (UnknownHostException e) {
			setDescription("Error: Lookup failed: Unknown host");
			return;
		} catch (IOException e) {
			setDescription("Error: " + e.getLocalizedMessage());
			return;
		}
		// See http://wiki.vg/Protocol (Status Ping)
		OutputStream out;
		InputStream in;
		try {
			out = socket.getOutputStream();
			in = socket.getInputStream();
			out.write(6 + serverAddress.length()); // length of packet ID + data
													// to follow
			out.write(0); // packet ID = 0
			out.write(4); // Protocol version
			out.write(serverAddress.length()); // varint length of server
												// address
			out.write(serverAddress.getBytes()); // server address in UTF8
			out.write((queryPort & 0xFF00) >> 8); // port number high byte
			out.write(queryPort & 0x00FF); // port number low byte
			out.write(1); // Next state: status
			out.write(0x01); // status ping (1st byte)
			out.write(0x00); // status ping (2nd byte)

			int packetLength = readVarInt(in); // size of entire packet
			String serverData;
			if (packetLength < 11) {
				Log.i(TAG, String.format("%s, %s: packet length too small: %d", serverName, serverAddress, packetLength));
				serverData = null;
				setDescription("Invalid response from server (server may be in the process of restarting, try again in a few seconds)");
				in.close();
				out.close();
				socket.close();
				return;
			}
			final int packetType = in.read(); // packet type - going to ignore it because we should
											  // only ever get the one type back anyway
			Log.d(TAG, String.format("%s, %s: packet type: %d", serverName, serverAddress, packetType));
			int jsonLength = readVarInt(in); // size of JSON blob
			if (jsonLength < 0){
				in.close();
				out.close();
				socket.close();
				return;
			}
			byte[] buffer = new byte[jsonLength + 10]; // a little more than
														// we're expecting just
														// to be safe
			int bytesRead = 0;
			do {
				bytesRead += in.read(buffer, bytesRead, jsonLength - bytesRead);
			} while (bytesRead < jsonLength);
			serverData = new String(buffer, 0, bytesRead);
			serverJSON = new JSONObject(serverData);
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private int readVarInt(InputStream in) {
		int theInt = 0;
//		int byteCounter = 0;

		for (int i = 0; i < 6; i++){
			int theByte;
			try {
				theByte = in.read();
			} catch (IOException e){
				e.printStackTrace();
				Log.w(TAG, "readVarInt: Failed to retrieve data from server");
				return 0;
			}

			theInt |= (theByte & 0x7F) << (7 * i);
//			if (byteCounter > 5) {
//				Log.w(TAG, String.format("readVarInt: invalid data received, %d bytes", byteCounter));
//				return -1;
//			}
			if (theByte == 0xffffffff){
				Log.w(TAG, String.format("readVarInt: received unexpected byte value: %#x", theByte));
				return -1;
			}
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

		double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE)
				: 1.0;

		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
		bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// optional
		return BitmapFactory.decodeByteArray(imageData, 0,
				imageData.length, bitmapOptions);
	}

	private static int getPowerOfTwoForSampleRatio(double ratio) {
		int k = Integer.highestOneBit((int) Math.floor(ratio));
		if (k == 0)
			return 1;
		else
			return k;
	}
}
