package org.theanticookie.bukkit.httpconsole;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;
import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.json.simple.JSONObject;

/**
 * Handles path /console POST/GET requests by executing the requested console
 * command on the minecraft console.
 *
 * @author BlueJeansAndRain
 */
public class ConsoleRequestHandler extends HTTPRequestHandler
{
	private class MinecraftLogHandler extends Handler
	{
		private final Writer writer;
		MinecraftLogHandler( final Writer writer )
		{
			this.writer = writer;
		}

		public void publish( LogRecord record )
		{
			try
			{
				writer.write( String.format( "%s\n", record.getMessage() ) );
			}
			catch( IOException e )
			{
				// Silently fail.
			}
		}

		public void close()
		{
			try
			{
				writer.close();
			}
			catch( IOException e )
			{
				// Silently fail.
			}
		}

		public void flush()
		{
			try
			{
				writer.flush();
			}
			catch( IOException e )
			{
				// Silently fail.
			}
		}
	}

	private CraftServer server;
	private ConsoleCommandSender sender;

	public ConsoleRequestHandler( final JavaPlugin plugin )
	{
		this.server = (CraftServer)plugin.getServer();
		this.sender = new ConsoleCommandSender( server );
	}

	private String executeConsoleCommand( String command )
	{
		log( "Executing \"%s\"", command );

		StringWriter command_output = new StringWriter();
		Logger minecraft_logger = Logger.getLogger( "Minecraft" );
		MinecraftLogHandler minecraft_log_handler = new MinecraftLogHandler( command_output );

		minecraft_logger.addHandler( minecraft_log_handler );
		server.dispatchCommand( sender, command );
		minecraft_logger.removeHandler( minecraft_log_handler );

		minecraft_log_handler.flush();
		minecraft_log_handler.close();

		return command_output.toString();
	}

	public boolean HandlePath( String path )
	{
		if ( path.equalsIgnoreCase( "/console" ) )
			return true;
		
		return false;
	}

	public boolean HandleRequest( HTTPRequest request, StringWriter output )
	{
		if ( !request.method.equals( "GET" ) && !request.method.equals( "POST" ) )
			return false;

		String command = "";
		if ( request.parameters.containsKey( "command" ) )
		{
			command = request.parameters.get( "command" ).trim();
		}
		else if (  request.data instanceof JSONObject )
		{
			JSONObject json = (JSONObject)request.data;
			try
			{
				command = (String)json.get( "command" );
			}
			catch ( Exception e )
			{
				// Silently fail.
			}
		}

		if ( command.equals( "" ) )
			return false;
		
		String command_output = executeConsoleCommand( command );
		if ( command_output.equals( "" ) )
			command_output = "\n";

		HTTPResponseHeaderHelper.outputHeaders( 200, output );
		output.write( command_output );

		return true;
	}
}
