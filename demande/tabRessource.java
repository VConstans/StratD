import java.util.*;

public class tabRessource{

	private Hashtable<String,Integer> ressource;

	public tabRessource()
	{
		ressource = new Hashtable<String,Integer>();
	}

	public int get(String key)
	{
		Integer i = ressource.get(key);

		if(ressource.containsKey(key))
		{
			return i.intValue();
		}
		else
		{
			return 0;
		}
	}

	public void put(String key,int value)
	{
		ressource.put(key,Integer.valueOf(value));
	}

	public boolean containsKey(String key)
	{
		return ressource.containsKey(key);
	}

	public boolean containsValue(int value)
	{
		return containsValue(Integer.valueOf(value));
	}

	public int size()
	{
		return ressource.size();
	}

	public Hashtable<String,Integer> getTab()
	{
		return ressource;
	}
}
