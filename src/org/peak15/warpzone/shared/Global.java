// A whole bunch of random crap needed by client and server.

package org.peak15.warpzone.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.peak15.warpzone.shared.ships.*;
import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryo.*;

public class Global {
	
	// Set true if this is running on client, false if running on the server.
	private static boolean CLIENT;
	private static boolean clientSet = false;
	public static void setClient() {
		if(!clientSet) {
			CLIENT = true;
			clientSet = true;
		}
		else {
			printErr("Invalid call to setClient().");
		}
	}
	public static void setServer() {
		if(!clientSet) {
			CLIENT = false;
			clientSet = true;
		}
		else {
			printErr("Invalid call to setServer().");
		}
	}
	public static boolean isClient() {
		if(!clientSet) {
			printErr("Client/Server not set.");
			System.exit(1);
		}
		return CLIENT;
	}
	public static boolean isServer() {
		if(!clientSet) {
			printErr("Client/Server not set.");
			System.exit(1);
		}
		return !CLIENT;
	}
	
	public static boolean DEBUG = true;
	
	public static final int TCP_PORT = 1337;
	public static final int UDP_PORT = 1337;
	public static final int MAP_PORT = 9001;
	public static final int WIDTH = 854;
	public static final int HEIGHT = 480;
	public static final int TILE_SIZE = 24;
	public static final int PACKET_SIZE = 512;
	public static final File CACHE_DIR = new File("cache/");
	
	public static final Vector CENTER = new Vector( (double) WIDTH/2.0, (double) HEIGHT/2.0);
	
	private static final String CONTENT_SERVER_DEBUG = "http://www.peak15.org/warpzone/content/";
	private static final String CONTENT_SERVER = "http://www.peak15.org.nyud.net/warpzone/content/";
	
	public static String getContentServer() {
		if(DEBUG)
			return CONTENT_SERVER_DEBUG;
		else
			return CONTENT_SERVER;
	}
	
	/**
	 * Converts a short to a byte table.
	 * @param s Short to convert.
	 * @return Byte table from short.
	 */
	public static byte[] toBytes(short s) {
        return new byte[]{(byte)(s & 0x00FF),(byte)((s & 0xFF00)>>8)};
    }
	
	/**
	 * Converts a byte table to a short.
	 * @param bytes Byte table to convert.
	 * @return Short from byte table.
	 */
	public static short toShort(byte[] bytes) {
		return (short)( ((bytes[1]&0xFF)<<8) | (bytes[0]&0xFF) );
	}
	
	/**
	 * Converts an int to a byte table.
	 * @param value Int to convert.
	 * @return Byte table from int.
	 */
	public static byte[] toBytes(int value) {
	    return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}
	
	/**
	 * Compresses a byte array with deflate.
	 * Ninja'd from: http://www.dscripts.net/2010/06/04/compress-and-uncompress-a-java-byte-array-using-deflater-and-enflater/
	 * @param data Data to compress.
	 * @return Compressed data.
	 * @throws IOException
	 * @author Burhan Uddin
	 */
	public static byte[] compressBytes(byte[] data) throws IOException {
		byte[] input = data; //the format... data is the total string
		Deflater df = new Deflater(); //this function mainly generate the byte code
		//df.setLevel(Deflater.BEST_COMPRESSION);
		df.setInput(input);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length); //we write the generated byte code in this array
		df.finish();
		byte[] buff = new byte[1024]; //segment segment pop....segment set 1024
		while(!df.finished())
		{
			int count = df.deflate(buff); //returns the generated code... index
			baos.write(buff, 0, count); //write 4m 0 to count
		}
		baos.close();
		byte[] output = baos.toByteArray();
		
		return output;
	}
	
	/**
	 * Decompress a byte array compressed with deflate.
	 * Ninja'd from: http://www.dscripts.net/2010/06/04/compress-and-uncompress-a-java-byte-array-using-deflater-and-enflater/
	 * @param input Byte array to decompress.
	 * @return Decompressed byte array.
	 * @throws IOException
	 * @throws DataFormatException
	 * @author Burhan Uddin
	 */
	public static byte[] extractBytes(byte[] input) throws IOException, DataFormatException {
		Inflater ifl = new Inflater(); //mainly generate the extraction
		//df.setLevel(Deflater.BEST_COMPRESSION);
		ifl.setInput(input);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
		byte[] buff = new byte[1024];
		while(!ifl.finished())
		{
			int count = ifl.inflate(buff);
			baos.write(buff, 0, count);
		}
		baos.close();
		byte[] output = baos.toByteArray();
		
		return output;
	}
	
	/**
	 * Splits a large byte table into a list of byte tables with max size PACKET_SIZE.
	 * @param bytes Byte table to split.
	 * @return List of byte tables with max size PACKET_SIZE.
	 */
	public static List<byte[]> toPackets(byte[] bytes) {
		List<byte[]> filePackets = new ArrayList<byte[]>();
		try {
			InputStream is = new ByteArrayInputStream(bytes);
			int offset = 0;
			while(offset < bytes.length) {
				byte[] bt;
				if((bytes.length - offset) < PACKET_SIZE) {
					bt = new byte[bytes.length - offset];
				}
				else
					bt = new byte[PACKET_SIZE];
				
				is.read(bt);
				
				offset += PACKET_SIZE;
				filePackets.add(bt);
			}
		    is.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return filePackets;
	}
	
	/**
	 * Combines a list of packets into one byte buffer.
	 * @param packets List of byte buffers to combine.
	 * @return Combined byte buffer.
	 */
	public static ByteBuffer combinePackets(List<ByteBuffer> packets) {
		int capacity = ((packets.size() - 1) * Global.PACKET_SIZE) + packets.get(packets.size() - 1).capacity();
		ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
		
		for(ByteBuffer packet : packets) {
			buffer.put(packet);
		}
		buffer.flip();
		return buffer;
	}
	
	/**
	 * Combines a list of packets into one byte array.
	 * @param packets List of byte buffers to combine.
	 * @return Combined byte array.
	 */
	public static byte[] combinePacketsToArray(List<ByteBuffer> packets) {
		int capacity = ((packets.size() - 1) * Global.PACKET_SIZE) + packets.get(packets.size() - 1).capacity();
		byte[] array = new byte[capacity];
		
		int offset = 0;
		for(ByteBuffer packet : packets) {
			byte[] packetA = new byte[packet.capacity()];
			packet.get(packetA);
			System.arraycopy(packetA, 0, array, offset, packetA.length);
			offset += Global.PACKET_SIZE;
		}
		
		return array;
	}
	
	// Universal print functions.
	public static void print(String string) {
		if(CLIENT) {
			org.peak15.warpzone.client.Shared.print(string);
		}
		else {
			org.peak15.warpzone.server.Shared.print(string);
		}
	}
	
	public static void printDbg(String string) {
		if(DEBUG) {
			if(CLIENT) {
				org.peak15.warpzone.client.Shared.printDbg(string);
			}
			else {
				org.peak15.warpzone.server.Shared.printDbg(string);
			}
		}
	}
	
	public static void printErr(String string) {
		if(CLIENT) {
			org.peak15.warpzone.client.Shared.printErr(string);
		}
		else {
			org.peak15.warpzone.server.Shared.printErr(string);
		}
	}
	
	// Kryo Register functions
	public static void register(Server server) {
		Kryo kryo = server.getKryo();
		reg(kryo);
	}
	
	public static void register(Client client) {
		Kryo kryo = client.getKryo();
		reg(kryo);
	}
	
	private static void reg(Kryo kryo) {
		kryo.register(JoinRequest.class);
		kryo.register(Player.class);
		kryo.register(Vector.class);
		kryo.register(DefaultShip.class);
		kryo.register(boolean[][].class);
		kryo.register(ArrayList.class);
	}
	
	// Returns a player from a player id
	/*public static Player getPlayer(byte id, ArrayList<Player> players) {
		Player temp;
		for(int i=0; i<players.size(); i++) { // find the right player
			temp = (Player)players.get(i);
			
			if(temp.id == id) {
				return temp;
			}
		}
		
		return null;
	}*/
}
