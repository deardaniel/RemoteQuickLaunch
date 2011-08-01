import java.io.Serializable;

import javax.swing.Icon;

public class Shortcut implements Serializable {
	private static final long serialVersionUID = -6406200971238048767L;
	
	private String name;
	private Icon icon;
	
	public Shortcut( String name, Icon icon )
	{
		this.name = name;
		this.icon = icon;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Icon getIcon()
	{
		return icon;
	}
}
