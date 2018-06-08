package dbs.redis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.lang3.ObjectUtils;
import redis.clients.jedis.Jedis;


public class JedisMaker {

	/**
	 * Make a Jedis object and authenticate it.
	 *
	 * @return
	 * @throws IOException
	 * @throws NullPointerException when can't locate file with redis credentials
	 */
	public static Jedis make() throws IOException, NullPointerException {
		
		// assemble the directory name
		String slash = File.separator;
		String filename = "resources" + slash + "redis_url.txt";
		URL fileURL = JedisMaker.class.getClassLoader().getResource(filename);
		String filepath = URLDecoder.decode(fileURL.getFile(), "UTF-8");
		
		// open the file
		StringBuilder sb = new StringBuilder();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filepath));
		} catch (FileNotFoundException e1) {
			System.out.println("File not found: " + filename);
			printInstructions();
			return null;
		}

		// read the file
		while (true) {
			String line = br.readLine();
			if (line == null) break;
			sb.append(line);
		}
		br.close();

		// parse the URL
		URI uri;
		try {
			uri = new URI(sb.toString());
		} catch (URISyntaxException e) {
			System.out.println("Reading file: " + filename);
			System.out.println("It looks like this file does not contain a valid URI.");
			printInstructions();
			return null;
		}
		String host = uri.getHost();
		int port = uri.getPort();

		String[] array = uri.getAuthority().split("[:@]");
		String auth = array[1];
		
		// connect to the server
		Jedis jedis = new Jedis(host, port);

		try {
			jedis.auth(auth);
		} catch (Exception e) {
			System.out.println("Trying to connect to " + host);
			System.out.println("on port " + port);
			System.out.println("with authcode " + auth);
			System.out.println("Got exception " + e);
			printInstructions();
			return null;
		}
		return jedis;
	}

	/**
	 *
	 */
	private static void printInstructions() {
		System.out.println("");
		System.out.println("To connect to RedisToGo, you have to provide a file called");
		System.out.println("redis_url.txt that contains the URL of your Redis server.");
		System.out.println("If you select an instance on the RedisToGo web page,");
		System.out.println("you should see a URL that contains the information you need:");
		System.out.println("redis://redistogo:AUTH@HOST:PORT");
		System.out.println("Create a file called redis_url.txt in the src/resources");
		System.out.println("directory, and paste in the URL.");
	}
}
