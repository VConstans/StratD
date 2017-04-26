import java.util.*;

public class tabRessource{

	private HashMap<String,Integer> ressource = new HashMap<String,Integer>();

	public int get(String key)
	{
		Integer i = ressource.get(key);

		if(i == null)
		{
			return -1;
		}
		else
		{
			return i.intValue();
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

	public HashMap<String,Integer> getTab()
	{
		return ressource;
	}
}
