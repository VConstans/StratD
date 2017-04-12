import java.util.TimerTask;
import StratD.Producteur;

public class ProductTask extends TimerTask
{
	ProducteurImpl prod;

	public ProductTask(ProducteurImpl p)
	{
		prod=p;
	}

	public void run()
	{
		prod.production(1);
	}
}
