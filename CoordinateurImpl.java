import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
import java.util.concurrent.locks.*;
import org.omg.CosNaming.*;
import StratD.Ressource;
import StratD.CoordinateurPOA;
import StratD.Coordinateur;
import StratD.JoueurPOA;
import StratD.Joueur;
import StratD.Producteur;
import StratD.ProducteurPOA;
import StratD.CoordinateurHelper;

public class CoordinateurImpl extends CoordinateurPOA
{

	ArrayList<Joueur> list_joueur = new ArrayList<Joueur>();
	ArrayList<Producteur> list_prod = new ArrayList<Producteur>();

	HashSet<String> list_ressource = new HashSet<String>();

	ArrayList<Ressource> list_besoin = new ArrayList();


	int maxJoueur;
	int maxProd;


	boolean RbR=false;

	Lock lock = new ReentrantLock();
	Condition demarrage = lock.newCondition();

	Lock tour = new ReentrantLock();
	Condition entrerTour = tour.newCondition();
	Condition finTour = tour.newCondition();

	boolean tourEnCours = false;


	public CoordinateurImpl(String s,int maxJ, int maxP)
	{
		System.out.println(s);
		if(s.equals("R"))
		{
			RbR=true;
		}
		maxJoueur=maxJ;
		maxProd=maxP;
	}


	synchronized public int ajoutJoueur(Joueur j, boolean modeDeJeu)
	{
		if(RbR != modeDeJeu)
		{
			return -2;
		}

		if(list_joueur.size()<maxJoueur)
		{
			list_joueur.add(j);
			System.out.println("======+> ajout joueur"+list_joueur.size());
			verificationCommencement();
			return list_joueur.size();
		}
		else
		{
			return -1;
		}
	}

	synchronized public int ajoutProd(Producteur p,boolean modeDeJeu,String ressourceType)
	{
		if(RbR != modeDeJeu)
		{
			return -2;
		}
		if(list_prod.size()<maxProd)
		{
			list_prod.add(p);
			System.out.println("======+> ajout prod"+list_prod.size());
			list_ressource.add(ressourceType);
			verificationCommencement();
			return list_prod.size();
		}
		else
		{
			return -1;
		}
	}

	public void ping(int id)
	{
		System.out.println(id+" connecté");
	}


	public void finTour()// throws InterruptedException
	{
		try
		{
		tour.lock();

		try
		{
			while(!tourEnCours)
			{
				finTour.await();
			}
			tourEnCours = false;
			entrerTour.signal();
		}
		finally
		{
			tour.unlock();
		}
		} catch (InterruptedException e)
		{ System.out.println("InterruptedException");}

	}


	private void commenceTour()// throws InterruptedException
	{
		try
		{
		tour.lock();
		try
		{
			while(tourEnCours)
			{
				entrerTour.await();
			}
			tourEnCours = true;
			finTour.signal();
		}
		finally
		{
			tour.unlock();
		}
		}catch(InterruptedException e)
		{ System.out.println("InterruptedException");}
	}


	private void verificationCommencement()
	{
		if(list_joueur.size() == maxJoueur && list_prod.size() == maxProd)
		{
			lock.lock();

			try{
				demarrage.signal();
			} finally {
				lock.unlock();
			}
		}
	}


	private void sendList()
	{
		Producteur[] tabProd =new Producteur[list_prod.size()];
		tabProd = list_prod.toArray(tabProd);

		Joueur[] tabJoueur = new Joueur[list_joueur.size()];
		tabJoueur = list_joueur.toArray(tabJoueur);

		String[] tabRessource = new String[list_ressource.size()];
		tabRessource = list_ressource.toArray(tabRessource);

		Ressource[] tabBesoin = new Ressource[list_besoin.size()];
		tabBesoin = list_besoin.toArray(tabBesoin);

		int i;
		
		for(i=0;i<list_joueur.size();i++)
		{
			list_joueur.get(i).rcvParametreJeu(tabJoueur, tabProd, tabRessource, tabBesoin, RbR);
		}
	}


	private void lancementJeu()
	{
		int i;

		for(i=0;i<list_prod.size();i++)
		{
			list_prod.get(i).lancementProduction();
		}

		for(i=0;i<list_joueur.size();i++)
		{
			list_joueur.get(i).gameLoop();
		}
	}

	private void boucleDeTour()
	{
			System.out.println("boucle");
		if(list_joueur.size() == 0)
		{
			return;
		}

		int i;

		while(true)
		{
			for(i=0;i<list_joueur.size();i++)
			{
				commenceTour();
				list_joueur.get(i).donneTour();
			}
			System.out.println("Milieu");
			for(i=0;i<list_prod.size();i++)
			{
				commenceTour();
				System.out.println("passe");
				list_prod.get(i).donneTour();
			}

		}
	}

	private void preparationJeu()
	{

		sendList();

		lancementJeu();

		if(RbR)
		{
			boucleDeTour();
		}
	}


	public static void main(String args[])
	{
		if (args.length != 5)
		{
			System.out.println("Usage : java ServeurChatImpl" + " <machineServeurDeNoms>" + " <No Port>" + " <R>" + " nb de joueur" + " nb de prod") ;
			return ;
		}
		try
		{
			String [] argv = {"-ORBInitialHost", args[0], "-ORBInitialPort", args[1]} ; 
			ORB orb = ORB.init(argv, null) ;
			CoordinateurImpl coord = new CoordinateurImpl(args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4])) ;

			// init POA
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA")) ;
			rootpoa.the_POAManager().activate() ;

			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(coord) ;
			Coordinateur href = CoordinateurHelper.narrow(ref) ;

			// inscription de l'objet au service de noms
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService") ;
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef) ;
			NameComponent path[] = ncRef.to_name( "Coordinateur" ) ;
			ncRef.rebind(path, href) ;

			System.out.println("Coordinateur ready and waiting ...") ;

			ThreadRun thread=new ThreadRun(orb);
			thread.start();

			coord.lock.lock();
			try{
			coord.demarrage.await();
			} finally {
			coord.lock.unlock();
			}

			System.out.println("après");

			coord.preparationJeu();


			thread.join();

		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e) ;
			e.printStackTrace(System.out) ;
		}
      
		System.out.println("Coordinateur Exiting ...") ;
	}
}
