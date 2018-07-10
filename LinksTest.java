package com.codepath;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LinksTest {
	private static String testHelper(String urlInput, String queryString, String requestMethod) throws IOException {
		URL url = new URL(urlInput);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setRequestMethod(requestMethod);
		
		if (requestMethod.equals("POST")) {
			httpCon.setDoOutput(true);
			OutputStream os = httpCon.getOutputStream();
			os.write(queryString.getBytes());
			os.flush();
			os.close();
		}
		int responseCode = httpCon.getResponseCode();
		String location = httpCon.getHeaderField("Location");
		String linkId = httpCon.getHeaderField("LinkId");
		System.out.println("Response Code: " + responseCode);
		
		if (location!=null)
			System.out.println("Location: " + location);
		
		return linkId;
	}
	
	
	public static void main(String[] args) throws IOException {	
		// Test GET
		System.out.println("Testing GET with link ID that doesn't exist.  Should 404.");
		testHelper("http://localhost:8080/Shortener/Links/nyt123", "", "GET");
		System.out.println("------------------------------");		
		
		// Test POST
		System.out.println("Testing POST with short link creation with ID. Should 201.");		
		String params = "url=https://www.nytimes.com&id=nyt123";
		testHelper("http://localhost:8080/Shortener/Links", params, "POST");
		System.out.println("------------------------------");

		// Test GET
		System.out.println("Testing GET with short link resolution with ID. Should 302 and redirect.");				
		testHelper("http://localhost:8080/Shortener/Links/nyt123", "", "GET");
		System.out.println("------------------------------");
		
		// Test POST
		System.out.println("Testing POST with short link creation without ID. Should 201.");						
		params = "url=https://news.google.com";		
		String createdLinkId = testHelper("http://localhost:8080/Shortener/Links", params, "POST");
		System.out.println("------------------------------");	
		
		// Test GET 
		System.out.println("Testing GET with short link resolution with ID " + createdLinkId+". Should 302 and redirect.");
		testHelper("http://localhost:8080/Shortener/Links/"+createdLinkId, "", "GET");
		System.out.println("------------------------------");
		
		// Test POST
		System.out.println("Testing POST with short link creation with same URL. Should 302 and return previously created ID.");		
		params = "url=https://news.google.com";
		testHelper("http://localhost:8080/Shortener/Links", params, "POST");
		System.out.println("------------------------------");			

		// Test POST
		System.out.println("Testing POST with conflicting short link creation. Should 409");		
		params = "url=http://www.metacritic.com/movie&id=nyt123";
		testHelper("http://localhost:8080/Shortener/Links", params, "POST");
		System.out.println("------------------------------");		

		// Test GET
		System.out.println("Testing GET with link ID that doesn't exist.  Should 404.");
		testHelper("http://localhost:8080/Shortener/Links/id-not-exist", "", "GET");
		System.out.println("------------------------------");		
	
		System.out.println("Testing rate limiting POST.  Should eventually 429.");		
		for (int i=0; i< 100; i++) {			
			testHelper("http://localhost:8080/Shortener/Links", "url=https://www.youtube.com", "POST");
		}
	
	}

}
