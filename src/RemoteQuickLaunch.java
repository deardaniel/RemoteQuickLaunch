import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class RemoteQuickLaunch
{
	private JFrame app = null;
	
	class JmDNSListModel implements ListModel, ServiceListener
	{
		private ArrayList<String> hosts = new ArrayList<String>();
		private ArrayList<ListDataListener> listeners = new ArrayList<ListDataListener>();
		String type;
		JmDNS jmdns = null;
		
		public JmDNSListModel( String type )
		{
			this.type = type;
			try {
				jmdns = JmDNS.create();
				jmdns.addServiceListener( type, this );
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		public void addListDataListener(ListDataListener l) {
			listeners.add( l );
		}

		public void removeListDataListener(ListDataListener l) {
			listeners.remove( l );
		}

		public Object getElementAt(int index) {
			return hosts.get( index );
		}

		public int getSize() {
			return hosts.size();
		}
		
		/*****/

		public void serviceAdded(ServiceEvent e) {
			System.out.println("Service added: " + e.getName());
			hosts.add( e.getName() );
			if ( !listeners.isEmpty() )
			{
				int i = hosts.indexOf( e.getName());
				ListDataEvent event = new ListDataEvent( this, ListDataEvent.INTERVAL_ADDED, i, i+1);
				for ( ListDataListener l : listeners )
				{
					l.intervalAdded( event );
				}
			}
		}

		public void serviceRemoved(ServiceEvent e) {
			System.out.println("Service removed: " + e.getName());
			int i = hosts.indexOf( e.getName());
			
			hosts.remove( e.getName() );
			if ( !listeners.isEmpty() )
			{
				ListDataEvent event = new ListDataEvent( this, ListDataEvent.INTERVAL_REMOVED, i, i+1);
				for ( ListDataListener l : listeners )
				{
					l.intervalRemoved( event );
				}
			}
		}

		public void serviceResolved(ServiceEvent e) {
			System.out.println("Service resolved: " + e.getInfo());
			startPicker( e.getInfo().getHostAddress() + ":" + e.getInfo().getPort() );
		}
		
		public void requestServiceInfo( String type, String name )
		{
			jmdns.requestServiceInfo( type, name, 5 );
		}
	}
	
	public static void main (String[] args)
	{
		new RemoteQuickLaunch();
	}
	
	public RemoteQuickLaunch()
	{
		final String type = "_rql._tcp.local.";
		
		app = new JFrame("Select host...");
		app.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		app.setSize( 200, 200);
		app.setLocationByPlatform( true );
		app.setResizable( false );
		
		final JmDNSListModel listModel = new JmDNSListModel( type );
		
		// host selection pane
		final JList list = new JList( listModel );
		list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		JScrollPane hosts = new JScrollPane( list );
		hosts.setPreferredSize( new Dimension( 50, 50) );

		// host text field
		final JTextField host = new JTextField();
		host.setText( getPref( "host", "" ) );
		host.setEnabled( false );
		
		// search radio button
		final JRadioButton search = new JRadioButton("Search for host");
		
		// specify radio button
		final JRadioButton specify = new JRadioButton("Specify host");

		ActionListener radioListener = new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				setPref("lastMode", search.isSelected() ? "search" : "specify" );
				list.setEnabled( !specify.isSelected() );
				host.setEnabled( specify.isSelected() );
			}
		};
		
		search.setSelected( getPref("lastMode", "search" ).equals("search") );
		specify.setSelected( !search.isSelected() );
		list.setEnabled( !specify.isSelected() );
		host.setEnabled( specify.isSelected() );
		
		
		specify.addActionListener( radioListener );
		search.addActionListener( radioListener );
		
		// radio button group
		ButtonGroup options = new ButtonGroup();
		options.add( search );
		options.add( specify );

		// Create top pane
		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.PAGE_AXIS ) );
		p.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10) );
		p.add( search );
		p.add( hosts );
		p.add( specify );
		p.add( host );
		
		// connect button
		JButton connect = new JButton("Connect");
		app.getRootPane().setDefaultButton( connect );
		connect.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( search.isSelected() ) {
					if (list.getSelectedIndex() == -1 )
						JOptionPane.showMessageDialog( app, "No host selected.", "Error", JOptionPane.ERROR_MESSAGE );
					else
						listModel.requestServiceInfo(type, list.getSelectedValue().toString() );
				} else if ( specify.isSelected() ) {
					if (!host.getText().matches("[a-zA-Z0-9.-]+(:\\d+)?") )
						JOptionPane.showMessageDialog( app, "Invalid host address.", "Error", JOptionPane.ERROR_MESSAGE );
					else
					{
						setPref( "host", host.getText() );
						startPicker( host.getText() );
					}
				}
			}
		});
		
		// Create bottom pane
		JPanel connectPanel = new JPanel();
		connectPanel.setLayout( new BoxLayout( connectPanel, BoxLayout.LINE_AXIS ) );
		connectPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 10, 10 ) );
		connectPanel.add( Box.createHorizontalGlue() );
		connectPanel.add( connect, BorderLayout.LINE_END );

		app.add( p, BorderLayout.PAGE_START );
		app.add( connectPanel, BorderLayout.PAGE_END );
		
		app.setVisible( true );
	}
	
	private void startPicker( String host )
	{
		try {
			Launcher launcher = (Launcher) Naming.lookup("rmi://"+host+"/Launcher");
			
			if ( launcher.passwordRequired() )
			{
				String passwordSHA1 = null;
				do {
					JPasswordField passwordField = new JPasswordField();
					JOptionPane.showConfirmDialog( app, passwordField, "Enter password:", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE );
					String password = new String(passwordField.getPassword());
					if ( password == null ) System.exit(0);
					
					passwordSHA1 = HashingUtil.hash( password );
				} while ( !launcher.auth( passwordSHA1 ) );

				app.setVisible( false );
				new ShortcutPicker( launcher, passwordSHA1 );
			} else {
				app.setVisible( false );
				new ShortcutPicker( launcher );
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
