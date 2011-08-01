import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class ShortcutPicker {
	private HashMap<JButton, Shortcut> map = new HashMap<JButton, Shortcut>();
	private JFrame app = null;
	private Launcher launcher = null;
	private String passwordSHA1 = null;
	
	public ShortcutPicker( Launcher launcher, String passwordSHA1 )
	{
		this.passwordSHA1 = passwordSHA1;
		this.launcher = launcher;
		
		ArrayList<Shortcut> shortcuts = null;
		try {
			shortcuts = launcher.getShortcuts( passwordSHA1 );
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		app = new JFrame("RemoteQuickLaunch");
		app.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		app.setLayout( new FlowLayout(FlowLayout.CENTER, 1, 1) );
		app.setLocationByPlatform( true );
		
		for ( Shortcut shortcut : shortcuts )
		{
			JButton button = new JButton();
			button.setPreferredSize( new Dimension( 40, 40) );
			button.setToolTipText( shortcut.getName().substring(0, shortcut.getName().length() - 4) );
			button.setIcon( shortcut.getIcon() );
			map.put(button, shortcut);
			
			button.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					launch( map.get((JButton)e.getSource()).getName() );
				}
			});
			app.add( button );
		}

		app.setSize( 44*5, (int)Math.ceil(((float)shortcuts.size()/5))*44 );
		app.setVisible( true );		
	}
	
	public ShortcutPicker( Launcher launcher ) {
		this( launcher, null );
	}

	private void setButtonsEnabled( boolean b )
	{
		int numButtons = app.getContentPane().getComponentCount();
		for ( int i=0; i < numButtons; i++ )
		{
			Component c = app.getContentPane().getComponent(i);
			if ( c instanceof JButton )
			{
				((JButton)c).setEnabled(b);
			}
		}
	}
	
	private void launch( String name )
	{
		setButtonsEnabled( false );
		try {
			launcher.launch( name, passwordSHA1 );
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		JOptionPane.showMessageDialog(app, "Launched "+name.substring(0, name.length()-4));
		setButtonsEnabled( true );
	}
}
