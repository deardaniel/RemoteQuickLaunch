import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import sun.awt.shell.ShellFolder;

public class LauncherImpl extends UnicastRemoteObject  implements Launcher
{
	private static final long serialVersionUID = -6619290499200652029L;
	String dirPath = System.getenv("APPDATA")+"\\Microsoft\\Internet Explorer\\Quick Launch\\";

	protected LauncherImpl() throws RemoteException {
		super();
	}

	public ArrayList<Shortcut> getShortcuts(String passwordSHA1) throws RemoteException
	{
		if ( !auth(passwordSHA1) ) return null;
		
		ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
		
		File dir = new File( dirPath );
		String[] files = dir.list();
		
		for ( String file : files )
		{
			if ( file.equals("desktop.ini") ||
				 file.equals("Window Switcher.lnk")) continue;
			
			File aFile = new File( dirPath + file );
			
			ShellFolder shellFolder = null;
			try {
				shellFolder = ShellFolder.getShellFolder( aFile );
				if ( shellFolder.isLink() )
					shellFolder = ShellFolder.getShellFolder( shellFolder.getLinkLocation() );
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			Icon icon = new ImageIcon(shellFolder.getIcon(true) );
			
			shortcuts.add( new Shortcut( file, icon ) );
		}
		
		return shortcuts;
	}

	public void launch( String file, String passwordSHA1 ) throws RemoteException
	{
		if ( !auth(passwordSHA1) ) return;
		
		try {
			Runtime.getRuntime().exec("cmd /C \""+dirPath+file+"\"");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean auth(String passwordSHA1) throws RemoteException {
		if ( !passwordRequired() ) return true;
		String realHash = Preferences.userNodeForPackage( RemoteQuickLaunchServer.class ).get( "passwordSHA1", "" );
		return realHash.equals( passwordSHA1 );
	}

	public boolean passwordRequired() throws RemoteException {
		return Preferences.userNodeForPackage( RemoteQuickLaunchServer.class ).getBoolean( "passwordRequired", false );
	}
}
