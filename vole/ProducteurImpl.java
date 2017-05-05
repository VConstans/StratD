import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
import java.io.*;
import java.io.IOException;
import java.util.concurrent.locks.*;
import org.omg.CosNaming.*;
import StratD.Coordinateur;
import StratD.CoordinateurHelper;
import StratD.Producteur;
import StratD.ProducteurPOA;
import StratD.ProducteurHelper;
import StratD.Ressource;


public class ProducteurImpl extends ProducteurPOA
{
	Producteur producteur;
	Coordinateur coord;
	ThreadRun thread;

	ProductTask pt;
	Timer timer;

	int id;
	String ressourceType;
	int nbRessource;
	int produit;

	boolean RbR = false;


	Lock tour = new ReentrantLock();
	Condition entrerTour = tour.newCondition();
	Condition finTour = tour.newCondition();


	Lock lock = new ReentrantLock();
	Condition terminaison = lock.newCondition();

	boolean mon_tour = true;

	public ProducteurImpl(String type,int nb)
	{
		ressourceType=type;
		nbRessource=nb;
		produit=0;

		pt=new ProductTask(this);
	}


	public void donneTour()
	{
		try
		{
		tour.lock();

		try
		{
			while(!mon_tour)
			{
				finTour.await();
			}
			mon_tour = false;
			entrerTour.signal();
		}
		finally
		{
			tour.unlock();
		}
		} catch (InterruptedException e)
		{ System.out.println("InterruptedException");}
	}


	public void rcvParametreJeu(boolean RbR)
	{
		this.RbR = RbR;
	}


	private void prendTour()// throws InterruptedException
	{
		try
		{
		tour.lock();
		try
		{
			while(mon_tour)
			{
				entrerTour.await();
			}
			mon_tour = true;
			finTour.signal();
		}
		finally
		{
			tour.unlock();
		}
		} catch (InterruptedException e)
		{ System.out.println("InterruptedException");}

	}



	synchronized public boolean demandeRessource(Ressource r)
	{
		if(r.type.equals(ressourceType) && r.nb <= produit)
		{
			produit-=r.nb;
			return true;
		}
		else
		{
			return false;
		}
	}

	synchronized public Ressource sondeProd()
	{
		return new Ressource(ressourceType,nbRessource);
	}

	public void annonce()
	{
		System.out.println("Prod");
	}

	public synchronized void production()
	{
		if(nbRessource >0)
		{
			int production = (nbRessource/2)+1;
			nbRessource -= production;
			produit+=production;
			System.out.println("Produit apres production :"+produit);
		}
	}

	public void lancementProduction()
	{
		System.out.println("prod");
		if(RbR)
		{
		System.out.println("R");
			while(true)
			{
				prendTour();
				System.out.println(id);
				production();
				coord.finTour();
			}
		}
		else
		{
		System.out.println("L");
			timer=new Timer();
			timer.scheduleAtFixedRate(pt,0,2*1000);
		}
	}



	public void terminaison()
	{
		lock.lock();
		try {
			terminaison.signal();
		} finally {
			lock.unlock();
		}
	}


	private void connection()
	{
		id = coord.ajoutProd(producteur, ressourceType);

		if(id == -1)
		{
				System.out.println("Plus de place disponible "+id);
		}
		else
		{
				coord.ping(id);

		}
	}



	public static void main(String args[])
	{
		ProducteurImpl prod = null ;

		if (args.length != 4)
		{
			System.out.println("Usage : java ClientChatImpl" + " <machineServeurDeNoms>" + " <No Port>" +"<num Ressource>" + "Nb de ressource") ;
			return ;
		}
		try
		{
			String [] argv = {"-ORBInitialHost", args[0], "-ORBInitialPort", args[1]} ; 
			ORB orb = ORB.init(argv, null) ;

			// Init POA
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA")) ;
			rootpoa.the_POAManager().activate() ;

			// creer l'objet qui sera appele' depuis le serveur
			prod = new ProducteurImpl(args[2],Integer.parseInt(args[3])) ;	//TODO changer parametre constructeur
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(prod) ;
			prod.producteur = ProducteurHelper.narrow(ref) ; 
			if (prod == null)
			{
				System.out.println("Pb pour obtenir une ref sur le client") ;
				System.exit(1) ;
			}

			// contacter le serveur
			String reference = "corbaname::" + args[0] + ":" + args[1] + "#Coordinateur" ;
			org.omg.CORBA.Object obj = orb.string_to_object(reference) ;

			// obtenir reference sur l'objet distant
			prod.coord = CoordinateurHelper.narrow(obj) ;
			if (prod.coord == null)
			{
				System.out.println("Pb pour contacter le serveur") ;
				System.exit(1) ;
			}
			else
			//	System.out.println("Annonce du serveur : " + client.serveur.ping()) ;

			// lancer l'ORB dans un thread
			prod.thread = new ThreadRun(orb) ;
			prod.thread.start() ;
			prod.connection();

			prod.lock.lock();
			try {
				prod.terminaison.await();
			} finally {
				prod.lock.unlock();
			}

			System.out.println("Fin");
		}
		catch (Exception e)
		{
			System.out.println("ERROR : " + e) ;
			e.printStackTrace(System.out) ;
		}
		finally
		{
			// shutdown
		//	if (prod != null)
		//	prod.thread.shutdown() ;

			System.exit(0);
		}
	}
	
}
