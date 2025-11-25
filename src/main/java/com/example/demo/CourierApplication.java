package com.example.demo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class CourierApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory("./")
				.load();

		String dbUrl = dotenv.get("DB_URL");
		String dbUsername = dotenv.get("DB_USERNAME");
		String dbPassword = dotenv.get("DB_PASSWORD");

		System.setProperty("DB_URL", dbUrl);
		System.setProperty("DB_USERNAME", dbUsername);
		System.setProperty("DB_PASSWORD", dbPassword);

		createDatabaseIfNotExists(dbUsername, dbPassword);

		ConfigurableApplicationContext context = SpringApplication.run(CourierApplication.class, args);
		printApplicationUrls(context);
	}

	private static void printApplicationUrls(ConfigurableApplicationContext context) {
		Environment env = context.getEnvironment();
		String port = env.getProperty("server.port", "8080");
		String contextPath = env.getProperty("server.servlet.context-path", "");

		System.out.println("\n" + "=".repeat(80));
		System.out.println("\n✓ Courier Management System Started Successfully!\n");
		System.out.println("🌐 Application URLs:");
		System.out.println("   ├─ Local:      http://localhost:" + port + contextPath + "/home");
		System.out.println("   ├─ Local IP:   http://127.0.0.1:" + port + contextPath + "/home");

		try {
			String hostAddress = InetAddress.getLocalHost().getHostAddress();
			System.out.println("   └─ Network:    http://" + hostAddress + ":" + port + contextPath + "/home");

			System.out.println("\n📡 Available Network Interfaces:");
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				if (ni.isUp() && !ni.isLoopback()) {
					Enumeration<InetAddress> addresses = ni.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress addr = addresses.nextElement();
						if (addr.getAddress().length == 4) {
							System.out.println("   ├─ " + ni.getDisplayName() + ": http://" + addr.getHostAddress()
									+ ":" + port + contextPath + "/home");
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
			String url = "jdbc:mysql://localhost:3306/?allowPublicKeyRetrieval=true&useSSL=false";
			java.sql.Connection conn = java.sql.DriverManager.getConnection(url, username, password);
			java.sql.Statement stmt = conn.createStatement();

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
