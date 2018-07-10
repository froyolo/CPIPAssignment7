package com.codepath;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Link shortener like bit.ly
 * Web service with an HTTP API that has 2 endpoints
 * 
 * BONUS: 
 * Links must persist upon app restart (using a database is not strictly a requirement)
 * Requests to create short links are limited to 100/hour
 */
@WebServlet("/Links/*")
public class Links extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String serializedMapName = "links.ser";
	private String serializedReverseMapName = "reversedlinks.ser";
	private String urlPath = "http://codepath.ly/";
	private Map<String, String> shortenedUrls; // Link map of short URLs-> full URLs
	private Map<String, String> reversedShortenedUrls;  // Reverse link map of full URLS -> short URLS
	private int counter;
	private long startTimer;
	
    public Links() {
        super();    
        
        // Deserialize link maps
        deserializeMaps();
        
        // Set counter and hour rate timer
        counter = 0;
        startTimer = System.currentTimeMillis();
    }

	/**
	 * GET /:linkId
	 * Resolves short link
	 * 
	 * Output:
	 * 302 Found - Location response header is the target URL
	 * 404 Not Found - if the specified link ID doesn't exist
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)  {
		try {
			String linkId = request.getPathInfo().substring(1);
			if (shortenedUrls.containsKey(linkId)) {
				String targetUrl = shortenedUrls.get(linkId);
				response.setHeader("Location", targetUrl);
				response.setStatus(HttpServletResponse.SC_FOUND); 
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}		
		} catch (Exception e) {
			System.out.println("Error occurred");
		}
	}

	/**
	 * POST /
	 * Create short link
	 * 
	 * Input:
	 * - url - the URL to shorten
	 * - id (optional) - a friendly link ID instead of a randomly generated one
	 * 
	 * Output:
	 * - 201 Created -if creation is successful. Location response header is the new short URL.  The 
	 * 		ID of the link is either the ID specified or a randomly generated ID consisting of 6
	 * 		alphanumeric characters
	 * - 409 Conflict -if id specified already exists
	 * - 429 Too Many Requests - if we have hit our global rate limt (see bonus)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)  {
		try {
			if (incrementRequestCount()) {								
				String url = request.getParameter("url");
				String id = request.getParameter("id");
				if (id!=null && shortenedUrls.containsKey(id)) {
					response.setStatus(HttpServletResponse.SC_CONFLICT);
				} else{
					if (id ==null) {
						if (reversedShortenedUrls.containsKey(url)) {
							id = reversedShortenedUrls.get(url);
						}  else {
							id = generateLinkId();
						}
					}
					shortenedUrls.put(id, url);
					reversedShortenedUrls.put(url, id);
					response.setStatus(HttpServletResponse.SC_CREATED); 
					response.setHeader("Location", urlPath+id);	
					response.setHeader("LinkId", id);
					
					// Serialize maps
					serializeMaps();
				}
			} else {
				response.setStatus(429);
			}			
		} catch (Exception e) {
			System.out.println("Error occurred");
		}
	}
	
	/*
	 * Deserializes the link maps
	 */
	private void deserializeMaps()  {
		System.out.println("Deserializing link maps..");
		
        try {
	        FileInputStream fileOutput = new FileInputStream(serializedMapName);
	        ObjectInputStream outputStream = new ObjectInputStream(fileOutput);
	        shortenedUrls = (HashMap<String, String>) outputStream.readObject();
	        outputStream.close();
	        fileOutput.close();
	        	        
	        FileInputStream reverseFileOutput = new FileInputStream(serializedReverseMapName);
	        ObjectInputStream reverseOutputStream = new ObjectInputStream(reverseFileOutput);
	        reversedShortenedUrls = (HashMap<String, String>) outputStream.readObject();
	        reverseOutputStream.close();
	        reverseFileOutput.close();
        } catch(Exception e) {
        	shortenedUrls = new HashMap<String, String>();
        	reversedShortenedUrls = new HashMap<String, String>();
        }	
	}	
	
	/**
	 * Serializes the link maps
	 */
	private void serializeMaps() throws IOException {
		// Serialize the link map 
		FileOutputStream fileOutput = new FileOutputStream(serializedMapName);
		ObjectOutputStream outputStream = new ObjectOutputStream(fileOutput);
		outputStream.writeObject(shortenedUrls);
		outputStream.close();
		fileOutput.close();
		
		// Serialize the reverse link map
		FileOutputStream reverseFileOutput = new FileOutputStream(serializedReverseMapName);
		ObjectOutputStream reverseOutputStream = new ObjectOutputStream(reverseFileOutput);
		reverseOutputStream.writeObject(reversedShortenedUrls);
		reverseOutputStream.close();
		reverseFileOutput.close();		
	}
	
	/**
	 * Rate limiting helper 
	 */
	private synchronized boolean incrementRequestCount() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - startTimer > 60*60*1000) {
			counter = 0;
			startTimer = currentTime;
		} 
		
		counter++;		
		return counter>100? false: true;
	}
	
	
	/**
	 * Helper to generate a random ID consisting of 6 alphanumeric characters
	 */
	private String generateLinkId() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		
		StringBuilder linkId = new StringBuilder();
		do {
			for (int i=0; i<6; i++ ) {
				linkId.append(chars.charAt(random.nextInt(chars.length())));
			}
		} while (shortenedUrls.containsKey(linkId.toString()));
		
		return linkId.toString();
	}	

}
