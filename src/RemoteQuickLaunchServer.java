import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.prefs.Preferences;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
//import javax.swing.UIManager;

public class RemoteQuickLaunchServer
{
	Launcher launcher = null;
	
	public static void main(String[] args)
	{
		new RemoteQuickLaunchServer();
	}
	
	public RemoteQuickLaunchServer()
	{
	    /*try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (Exception e) {
			e.printStackTrace();
	    }*/
	    
		final int port = 1099;
		try {
			System.out.println("Initialising RMI...");
			LocateRegistry.createRegistry( port );
			launcher = new LauncherImpl();
			Naming.rebind("rmi://localhost/Launcher", launcher);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not initialise.\nPerhaps "+getClass().getSimpleName()+" is already running?", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}

		try {
			System.out.println("Advertising...");
			JmDNS jmdns = JmDNS.create();
			ServiceInfo service = ServiceInfo.create("_rql._tcp.local.", InetAddress.getLocalHost().getHostName(), port, "");
			jmdns.registerService( service );
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not advertise the service.\nRemote users may not be able to automatically discover this machine.", "Error", JOptionPane.ERROR_MESSAGE );
			e.printStackTrace();
		}

		final Image imageBig = Toolkit.getDefaultToolkit().getImage("Icon-64.png");
		final Image imageSmall = Toolkit.getDefaultToolkit().getImage("Icon-16.png");
		
		// Create menu for tray icon
	    PopupMenu popup = new PopupMenu();
	    MenuItem aboutItem = new MenuItem("About");
	    final MenuItem changePasswordItem = new MenuItem("Change Password");
		changePasswordItem.setEnabled( getPref( "passwordRequired", false ) );
	    final MenuItem passwordItem = new MenuItem( (getPref("passwordRequired", false) ? "Disable" : "Enable" ) +" Password");
	    MenuItem exitItem = new MenuItem("Exit");
	    
	    aboutItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Icon icon = new ImageIcon( imageBig );
	            JOptionPane.showConfirmDialog(null, "RemoteQuickLaunch Server\nDaniel Heffernan, ©2009\nhttp://daniel.ie/", getClass().getSimpleName(), JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, icon);
	        }
	    });
	    
	    changePasswordItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
	           changePassword();
			}
	    });
	    
	    passwordItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				boolean passwordRequired = getPref( "passwordRequired", false );
				int action = JOptionPane.showConfirmDialog(null, (passwordRequired ? "Disable" : "Enable") + " password?", "Input", JOptionPane.YES_NO_OPTION );
				if ( action < 0 || action == JOptionPane.NO_OPTION) return;
				
				passwordRequired = !passwordRequired;
				setPref("passwordRequired", passwordRequired);
				if ( passwordRequired && getPref("passwordSHA1", "").isEmpty() )
					changePassword();
	            
				changePasswordItem.setEnabled( passwordRequired );
	    	    passwordItem.setLabel( ( passwordRequired ? "Disable" : "Enable" ) +" Password");
	        }
	    });
	    
	    exitItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
	            System.out.println("Exiting...");
	            System.exit(1);	// for some reason the JVM blocks indefinitely if this is 0
	        }
	    });
	    
	    popup.add(aboutItem);
	    popup.addSeparator();
	    popup.add(changePasswordItem);
	    popup.add(passwordItem);
	    popup.addSeparator();
	    popup.add(exitItem);

	    // Create tray icon
	    final TrayIcon trayIcon = new TrayIcon(imageSmall, "RemoteQuickLaunchServer", popup);
	    trayIcon.setImageAutoSize(true);

	    // Add icon to tray
		try {
			SystemTray.getSystemTray().add( trayIcon );
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	private void changePassword()
	{
		JPasswordField passwordField = new JPasswordField();
		
		int action;
		
		action = JOptionPane.showConfirmDialog(null, passwordField, "Enter password:", JOptionPane.OK_CANCEL_OPTION );
		if ( action < 0 || action == JOptionPane.CANCEL_OPTION ) return;
		String password1 = new String( passwordField.getPassword() );
		passwordField.setText("");
		action = JOptionPane.showConfirmDialog(null, passwordField, "Confirm password:", JOptionPane.OK_CANCEL_OPTION );
		if ( action < 0 || action == JOptionPane.CANCEL_OPTION ) return;
		String password2 = new String( passwordField.getPassword() );
        
        if ( password1.equals( password2 ) )
        {
        	setPref("passwordSHA1", HashingUtil.hash(password1) );
        	JOptionPane.showMessageDialog(null, "Password was successfully set.", "Information", JOptionPane.INFORMATION_MESSAGE );
        }
        else
        {
        	JOptionPane.showMessageDialog(null, "Passwords did not match.", "Error", JOptionPane.ERROR_MESSAGE );
        }
	}
	
	private void setPref( String key, boolean value )
	{
		Preferences.userNodeForPackage( getClass() ).putBoolean( key, value );
	}
	
	private boolean getPref( String key, boolean def )
	{
		return Preferences.userNodeForPackage( getClass() ).getBoolean( key, def );
	}
	
	private void setPref( String key, String value )
	{
		Preferences.userNodeForPackage( getClass() ).put( key, value );
	}
	
	private String getPref( String key, String def )
	{
		return Preferences.userNodeForPackage( getClass() ).get( key, def );
	}
}
