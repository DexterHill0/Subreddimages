package com.subreddimages;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.StringWriter;
import java.io.PrintWriter;

import javax.swing.JOptionPane;

public class _Logger {

	static Logger MainLogger = Logger.getLogger("global");
	static StringWriter sw = new StringWriter();
	static PrintWriter pw = new PrintWriter(sw);
	static FileHandler fh;

	/*
	 * This function initialises the logger.
	 * It also allows the logger to write to a log file.
	 */
	public static void initialize() 
	{
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-1s] %5$s %n");
		try {  
			fh = new FileHandler(System.getProperty("user.dir") + "/latestlog.log");
			MainLogger.addHandler(fh);
	        fh.setLevel(Level.ALL);
	        MainLogger.setLevel(Level.ALL); 
	        SimpleFormatter formatter = new SimpleFormatter();
	        fh.setFormatter(formatter);

	    } catch (Exception e) {  
	    	fullLog(e); 
	    }
		
		Log("Logger succesfully initialized!", Level.INFO);
	}
	
	/*
	 * Just logs text (both to the logger and to the console)
	 */
	public static void Log(String text, Level level) {
		MainLogger.log(level, text);
		fh.flush();
	}
	
	/*
	 * Closes logger
	 */
	public static void Close() {
		fh.flush();
		fh.close();
	}
	
	/*
	 * FullLog() logs to the Logger, Console, and creates a dialog about the error.
	 */
	public static void fullLog(Exception e) {
		e.printStackTrace(pw);
    	String sStackTrace = sw.toString(); //Convert stack trace to string
    	Functions.infoBox("Error: "+e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    	MainLogger.log(Level.SEVERE, sStackTrace);
	}
	
	/*
	 * PartialLog() logs to the Logger and console, but doesn't create a dialog.
	 */
	public static void partialLog(Exception e) {
		e.printStackTrace(pw);
    	String sStackTrace = sw.toString();
    	MainLogger.log(Level.SEVERE, sStackTrace);
	}

	
}
