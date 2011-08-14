package org.theanticookie.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.regex.Pattern;
//import java.util.HashMap;
//import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.util.config.Configuration;
import org.theanticookie.bukkit.httpconsole.ConsoleRequestHandler;
import org.theanticookie.bukkit.httpconsole.HTTPServer;
import org.theanticookie.bukkit.httpconsole.LogFilterConsoleCommandSender;
import org.theanticookie.bukkit.httpconsole.LogFilterLevel;
import org.theanticookie.bukkit.httpconsole.ResourceManager;

/**
 * HTTPConsole plugin for Bukkit/Minecraft
 *
 * @author BlueJeansAndRain
 */
public class HTTPConsole extends JavaPlugin
{
	public static boolean debugging = false;

	//private static final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
	private static final Logger logger = Logger.getLogger( "org.bukkit.BlueJeansAndRain.HTTPConsole" );

	private static PluginDescriptionFile package_description = null;
	private static ResourceManager resource_manager = null;
	private LogFilterConsoleCommandSender console_filter = null;
	private HTTPServer http = null;

	public static String getVersion()
	{
		return package_description.getVersion();
	}

	public static String getName()
	{
		return package_description.getName();
	}
	
	private static String formatLogMessage( String message, Object ... params )
	{
		return String.format( "%s: %s", getName(), String.format( message, params ) );
	}

	public static void rawLog( String message, Object ... params )
	{
		System.out.println( String.format( message, params ) );
	}

	public static void debug( String message, Object ... params )
	{
		if ( !debugging )
			return;

		rawLog( message, params );
	}

	public static void log( String message, Object ... params )
	{
		System.out.println( formatLogMessage( message, params ) );
	}

	public static void log( Level level, String message, Object ... params )
	{
		logger.log( level, formatLogMessage( message, params ) );
	}

	public static void logException( Exception e )
	{
		logger.log( Level.SEVERE, e.getMessage(), e );
	}

	public static void logException( Exception e, String message, Object ... params )
	{
		log( Level.SEVERE, message, params );
		logException( e );
	}
	
	public void enablePlugin()
	{
		this.getServer().getPluginManager().enablePlugin( this );
	}

	public void disablePlugin()
	{
		this.getServer().getPluginManager().disablePlugin( this );
	}

	public boolean generateDefaultConfigFile( File file )
	{
		try
		{
			resource_manager.writeResourceToFile( "/config.yml", file );
		}
		catch ( IOException e )
		{
			logException( e, "Failed to create config.yml" );
			return false;
		}

		return true;
	}

	public Configuration getConfig()
	{
		File config_file = new File( this.getDataFolder(), "config.yml" );
		Configuration config = new Configuration( config_file );

        if ( !this.getDataFolder().exists() )
		{
			if ( !getDataFolder().mkdirs() )
			{
				log( Level.SEVERE, "Error creating data folder" );
			}
			else
			{
				generateDefaultConfigFile( config_file.getAbsoluteFile() );
			}
		}
		else if ( !config_file.exists() )
		{
			generateDefaultConfigFile( config_file.getAbsoluteFile() );
		}

		config.load();
		return config;
	}

    public void onEnable()
	{
		if ( package_description == null )
			package_description = getDescription();
		if ( resource_manager == null )
			resource_manager = new ResourceManager();
		
		Configuration config = getConfig();
		debugging = config.getBoolean( "debug", false );
		
		logger.setFilter( new LogFilterLevel( config.getString( "log-level", "severe" ) ) );

		console_filter = new LogFilterConsoleCommandSender();
		if ( config.getBoolean( "filter-command-sender", true ) )
		{
			// Filter out annoying duplicate messages sent by
			// ConsoleCommandSender.
			// * It's possible someone else will set the filter removing this
			//   one, but the worst that will happen is ConsoleCommandSender
			//   messages will be visible again.
			Logger.getLogger( "Minecraft" ).setFilter( console_filter );
		}

		try
		{
			http = new HTTPServer();
			
			List<String> whitelist = config.getStringList( "client-ip-whitelist", null );
			for ( String ip : whitelist )
				http.addToWhitelist( ip );

			List<String> blacklist = config.getStringList( "client-ip-blacklist", null );
			for ( String ip : blacklist )
				http.addToBlacklist( ip );

            http.setDenyBeforeAllow( Pattern.matches(
                "^(?i)\\s*deny\\s*,\\s*allow\\s*$",
                config.getString( "white-black-list-order", "Deny,Allow")
            ));

			List<String> allowed_hosts = config.getStringList( "allowed-hosts", null );
			for ( String host : allowed_hosts )
				http.addAllowedHost( host );

			http.addRequestHandler( new ConsoleRequestHandler( this ) );
			http.setAlwaysLogRefusedConnections( config.getBoolean( "always-log-refused-connections", false ) );
			http.start( config.getString( "ip-address", "127.0.0.1" ), config.getInt( "port", 8765 ) );
		}
		catch( Exception e )
		{
			logException( e, "Error creating HTTP server" );
			disablePlugin();
			return;
		}
		
		rawLog( "%s %s is enabled", getName(), getVersion() );
    }

	// NOTE: All registered events are automatically unregistered when a plugin is disabled
    public void onDisable()
	{
        http.stopServer();

		// Remove the ConsoleCommandSender filter if it hasn't been overwritten.
		Logger minecraft_logger = Logger.getLogger( "Minecraft" );
		if ( minecraft_logger.getFilter() == console_filter )
			minecraft_logger.setFilter( null );

        rawLog( "%s %s is disabled", getName(), getVersion() );
    }
	
    /*public boolean isDebugging( final Player player )
	{
        if ( debugees.containsKey( player ) )
            return debugees.get( player );
        else
            return false;
    }

    public void setDebugging(final Player player, final boolean value)
	{
        debugees.put( player, value );
    }*/
}
