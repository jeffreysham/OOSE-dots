//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
// and Prof. Smith from JHU OOSE Fall 2016 
//-------------------------------------------------------------------------------------------------------------//

package com.oose2016.jsham2.dots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

import static spark.Spark.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class sets up the ports and the IP address. It also
 * sets up the database file.
 * 
 * @author jsham2, Jeffrey Sham CS421
 *
 */
public class Bootstrap {
	// The IP address
	public static final String IP_ADDRESS = "localhost";
	
	// The port number
	public static final int PORT = 8080;
	
	// The logger
	private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
	
	/**
	 * This method sets up the ports, IP address, and database file.
	 * @param args
	 */
	public static void main(String[] args) {
		//Check for database file
		DataSource dataSource = configureDataSource();
		if (dataSource == null) {
			System.out.printf("Could not find dots.db in current directory: %s", 
					Paths.get(".").toAbsolutePath().normalize());
			System.exit(1);
		}
		
		ipAddress(IP_ADDRESS);
		port(PORT);
		
		staticFileLocation("/public");
		
		try {
			DotsService service = new DotsService(dataSource);
			new DotsController(service);
		} catch (DotsService.DotsServiceException ex) {
			logger.error("Failed to create Dots instance. Aborting.");
		}
	}
	
	/**
	 * Check if the database file exists. Returns
	 * a newly created DataSource instance for the
	 * file if it exists 
	 * @return javax.sql.DataSource corresponding to the dots database
	 */
	private static DataSource configureDataSource() {
		Path dotsPath = Paths.get(".", "dots.db");
		if (!Files.exists(dotsPath)) {
			try {
				Files.createFile(dotsPath);
			} catch (IOException ex) {
				logger.error("Could not create dots.db file.");
			}
		}
		
		SQLiteDataSource dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:dots.db");
		return dataSource;
	}
}
