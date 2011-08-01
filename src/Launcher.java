import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Launcher extends Remote
{
	public ArrayList<Shortcut> getShortcuts(String passwordSHA1) throws RemoteException;
	public void launch( String file, String passwordSHA1 ) throws RemoteException;
	public boolean passwordRequired() throws RemoteException;
	public boolean auth( String passwordSHA1 ) throws RemoteException;
}
