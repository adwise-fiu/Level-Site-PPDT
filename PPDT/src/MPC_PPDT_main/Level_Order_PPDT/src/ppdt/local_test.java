package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.io.FileReader;
import java.util.Iterator;

public class local_test {
	
	public static void main(String [] args) throws Exception {
		// Read Config file
		JSONParser parser = new JSONParser();
		try {
			Object obj = parser.parse(new FileReader("../../Data/config.json"));
			JSONObject jsonObject = (JSONObject)obj;
			String name = (String)jsonObject.get("Name");
			String course = (String)jsonObject.get("Course");
			JSONArray subjects = (JSONArray)jsonObject.get("Subjects");
			System.out.println("Name: " + name);
			System.out.println("Course: " + course);
			System.out.println("Subjects:");
			Iterator iterator = subjects.iterator();
			while (iterator.hasNext()) {
				System.out.println(iterator.next());
			}
		} 
		catch(Exception e) {
			e.printStackTrace();
		}
		
		// Create Level sites
    	level_site_server server = new level_site_server(9000);
    	new Thread(server).start();
    	server.stop();
    	
		// Create the server
		server_site cloud = new server_site(null, args, null);
    	new Thread(cloud).start();
		
		// Create client
    	client evaluate = new client(0, null, args, null, 0);
    	new Thread(evaluate).start();
    	
    	// Close the Level Sites
	}
}
