package com.example.demo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import io.github.cdimascio.dotenv.Dotenv;

//Hibernate SessionFactory is managed automatically by Spring Boot when you use spring-boot-starter-data-jpa.
//Session: trnsaction in a database
//EntityManager is the interface , high level abstraction,part of JPA
//┌───────────────────────────┐
// │      Spring Boot App       │
// │  (Your code: Repositories, │
// │   Entities, Controllers)   │
// └─────────────▲──────────────┘
//               │
//               │
//               │
//    Spring Data JPA (JpaRepository)
//               │
//               ▼
// ┌───────────────────────────┐
// │   EntityManagerFactory     │
// │  (Manages EntityManager)   │
// │       (JPA level)          │
// └─────────────▲──────────────┘
//               │
//               │
//               ▼
// ┌───────────────────────────┐
// │    Hibernate SessionFactory│
// │    (Manages Sessions)      │
// │        (Hibernate level)   │
// └─────────────▲──────────────┘
//               │
//               │
//               ▼
//      ┌─────────────────────┐
//      │     Database         │
//      │   (MySQL, etc.)      │
//      └─────────────────────┘
// Explanation:
// Spring Boot Application:

// This includes your application code such as repositories (e.g., JpaRepository), entities (JPA annotated classes), and controllers. You typically work with JPA repositories to interact with the database.
// Spring Data JPA:

// Spring Data JPA provides an abstraction over JPA to handle database operations. It integrates with the EntityManager to interact with the database.
// EntityManagerFactory:

// Spring Boot automatically configures the EntityManagerFactory, which is responsible for creating EntityManager instances. The EntityManager is used to persist and retrieve entities in JPA. Internally, it delegates these operations to Hibernate.
// Hibernate SessionFactory:

// The SessionFactory is part of Hibernate, which manages Session instances. The Session represents a connection to the database and handles tasks like querying and saving data. The EntityManagerFactory creates and manages the SessionFactory.
// Database:

// Finally, the SessionFactory (via sessions) communicates with the database (such as MySQL) to perform the actual persistence operations.
// In summary, you primarily interact with the JPA repository or EntityManager in your Spring Boot application, while Hibernate’s SessionFactory works behind the scenes to manage sessions and database transactions.

@SpringBootApplication
public class CourierApplication {

	public static void main(String[] args) {

		// Load the .env file
        Dotenv dotenv = Dotenv.configure()
                              .directory("./")  // Specify the directory where .env is located
                              .load();

        // Set environment variables
        String dbUrl = dotenv.get("DB_URL");
        String dbUsername = dotenv.get("DB_USERNAME");
        String dbPassword = dotenv.get("DB_PASSWORD");
        
        System.setProperty("DB_URL", dbUrl);
        System.setProperty("DB_USERNAME", dbUsername);
        System.setProperty("DB_PASSWORD", dbPassword);

		// Create database if it doesn't exist
		createDatabaseIfNotExists(dbUsername, dbPassword);

		ConfigurableApplicationContext context = SpringApplication.run(CourierApplication.class, args);
		
		// Print application URLs
		printApplicationUrls(context);
	}
	
	private static void printApplicationUrls(ConfigurableApplicationContext context) {
		Environment env = context.getEnvironment();
		String port = env.getProperty("server.port", "8080");
		String contextPath = env.getProperty("server.servlet.context-path", "");
		
		System.out.println("\n" + "=".repeat(80));
		System.out.println("\n✓ Courier Management System Started Successfully!\n");
		System.out.println("🌐 Application URLs:");
		System.out.println("   ├─ Local:      http://localhost:" + port + contextPath);
		System.out.println("   ├─ Local IP:   http://127.0.0.1:" + port + contextPath);
		
		try {
			String hostAddress = InetAddress.getLocalHost().getHostAddress();
			System.out.println("   └─ Network:    http://" + hostAddress + ":" + port + contextPath);
			
			// Print all network interfaces
			System.out.println("\n📡 Available Network Interfaces:");
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				if (ni.isUp() && !ni.isLoopback()) {
					Enumeration<InetAddress> addresses = ni.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress addr = addresses.nextElement();
						if (addr.getAddress().length == 4) { // IPv4 only
							System.out.println("   ├─ " + ni.getDisplayName() + ": http://" + addr.getHostAddress() + ":" + port + contextPath);
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Could not determine network address: " + e.getMessage());
		}
		
		System.out.println("\n⚡ Features:");
		System.out.println("   ├─ Session Management: Enabled (30 min timeout)");
		System.out.println("   ├─ Database: MySQL (courier_db)");
		System.out.println("   └─ Dashboard: Modern UI with real-time tracking\n");
		System.out.println("=".repeat(80) + "\n");
	}

	private static void createDatabaseIfNotExists(String username, String password) {
		try {
			// Connect to MySQL server without specifying database
			String url = "jdbc:mysql://localhost:3306/?allowPublicKeyRetrieval=true&useSSL=false";
			java.sql.Connection conn = java.sql.DriverManager.getConnection(url, username, password);
			java.sql.Statement stmt = conn.createStatement();
			
			// Create database
			stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS courier_db");
			System.out.println("✓ Database 'courier_db' is ready!");
			
			stmt.close();
			conn.close();
		} catch (Exception e) {
			System.err.println("Warning: Could not create database - " + e.getMessage());
			System.err.println("The application will try to connect anyway...");
		}
	}

}
