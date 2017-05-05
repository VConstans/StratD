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
import StratD.Transaction;

public class CoordinateurImpl extends CoordinateurPOA
{

	ArrayList<Joueur> list_joueur = new ArrayList<Joueur>();
	ArrayList<Producteur> list_prod = new ArrayList<Producteur>();

	HashSet<String> list_ressource = new HashSet<String>();

	ArrayList<Ressource> list_besoin = new ArrayList<Ressource>();

	tabRessource[] ressource_joueur_fini;
	ArrayList<Integer> joueur_fini = new ArrayList<Integer>();
	Hashtable<Integer,Integer> nbRessourceParJoueur = new Hashtable<Integer,Integer>();

	int[] classement;
	int positionDernierClassement =0;


	int joueurStopper = 0;

	ArrayList<Transaction> listTransaction = new ArrayList<Transaction>();


	int maxJoueur;
	int maxProd;


	boolean RbR=false;
	int modeDeFin;
	int modeEval;

	Lock lock = new ReentrantLock();
	Condition demarrage = lock.newCondition();
	Condition terminaison = lock.newCondition();

	Lock tour = new ReentrantLock();
	Condition entrerTour = tour.newCondition();
	Condition finTour = tour.newCondition();

	boolean tourEnCours = false;


	public CoordinateurImpl(String s,String mdf,String me, int maxJ, int maxP)
	{
		maxJoueur=maxJ;
		maxProd=maxP;

		if(s.equals("R"))
		{
			RbR=true;
		}


		if(mdf.equals("A"))
		{
			modeDeFin=2;
		}
		else if(mdf.equals("F"))
		{
			modeDeFin=1;
		}


		if(me.equals("P"))
		{
			modeEval=1;
		}
		else if(me.equals("R"))
		{
			modeEval=0;
		}
	}


	synchronized public int ajoutJoueur(Joueur j)
	{
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

	synchronized public int ajoutProd(Producteur p,String ressourceType)
	{
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


	synchronized public void finJoueur(int id, Transaction[] tabTransaction)
	{
		//TODO faire classement en fct du mode de jeu
		if(modeEval == 1)
		{
			classement[positionDernierClassement] = id;
			positionDernierClassement+=1;
		}

		joueur_fini.add(new Integer(id));
		
		traitementTransaction(tabTransaction);

		if((modeDeFin == 1 && joueur_fini.size() == 1) || modeDeFin == 2 && joueur_fini.size() == list_joueur.size())
		{
			int i;

			for(i=0;i<list_joueur.size();i++)
			{
				list_joueur.get(i).arretJoueur();
			}
		}
	}

	private void traitementTransaction(Transaction[] tab)
	{
		int i;
		for(i=0;i<tab.length;i++)
		{
			System.out.println(tab[i].timeStamp+","+tab[i].emetteur+","+tab[i].recepteur+","+tab[i].ressource.type+","+tab[i].ressource.nb+","+tab[i].vole+","+tab[i].penalise);
		}
	}


	public void recuperationRessourceJoueur(int id, String r, int nb)
	{
		if(ressource_joueur_fini[id-1] == null)
			System.out.println("====================================> null");
		ressource_joueur_fini[id -1].put(r,nb);
	}


	public void signalJoueurStopper()
	{
		joueurStopper+=1;

		if(joueurStopper == list_joueur.size())
		{
			//TODO afficher classement et arreter programme
			if(modeEval == 0)
			{
				calculClassement();
			}
			terminaisonJoueur();
			terminaisonProd();
			terminaisonCoord();
		}
	}


	private void terminaisonJoueur()
	{
		int i;

		for(i=0;i<list_joueur.size();i++)
		{
			list_joueur.get(i).terminaison();
		}
	}

	private void terminaisonProd()
	{
		int i;

		for(i=0;i<list_prod.size();i++)
		{
			list_prod.get(i).terminaison();
		}
	}

	private void terminaisonCoord()
	{
	
		System.out.println("UNLOCK");
		lock.lock();
		try {
			terminaison.signal();
		} finally {
			lock.unlock();
		}
	}


	private void calculClassement()
	{
		Hashtable<Integer,Integer> tmp = new Hashtable<Integer,Integer>();

		int i;
		int somme =0;

		for(i=0; i<list_joueur.size();i++)
		{
			for(Map.Entry<String,Integer> entree : ((ressource_joueur_fini[i].getTab()).entrySet()))
			{
				//TODO
				somme+=entree.getValue().intValue();
			}
			nbRessourceParJoueur.put(new Integer(i+1),new Integer(somme));
			tmp.put(new Integer(i+1),new Integer(somme));
			somme=0;
		}

		//TODO copier si on veut grader les totaux des ressources


		int max = 0;
		int idMax = 0;

		for(i=0;i<list_joueur.size();i++)
		{
			for(Map.Entry<Integer,Integer> entree : (tmp.entrySet()))
			{
				if(entree.getValue().intValue() >= max)
				{
					max = entree.getValue().intValue();
					idMax = entree.getKey().intValue();
				}
			}

			classement[i]=idMax;
			tmp.remove(new Integer(idMax));
		}


		System.out.println("Classement:");




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


		generationListeBesoin(tabRessource);


		Ressource[] tabBesoin = new Ressource[list_besoin.size()];
		tabBesoin = list_besoin.toArray(tabBesoin);

		int i;
		
		for(i=0;i<list_joueur.size();i++)
		{
			list_joueur.get(i).rcvParametreJeu(tabJoueur, tabProd, tabRessource, tabBesoin, RbR);
		}

		for(i=0;i<list_prod.size();i++)
		{
			list_prod.get(i).rcvParametreJeu(RbR);
		}
	}


	private void generationListeBesoin(String[] ressource)
	{
		int i;

		for(i=0;i<ressource.length;i++)
		{
			int besoin = (int)(Math.random()*(float)80);
			Ressource r = new Ressource(ressource[i],besoin);
			list_besoin.add(r);
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
		if(list_joueur.size() == 0)
		{
			return;
		}

		int i;

		while(joueur_fini.size() != list_joueur.size())
		{
			for(i=0;i<list_joueur.size();i++)
			{
				if(!joueur_fini.contains(new Integer(i+1)))
				{
					commenceTour();
					list_joueur.get(i).donneTour();
				}
			}
			for(i=0;i<list_prod.size();i++)
			{
				commenceTour();
				list_prod.get(i).donneTour();
			}

		}

		
	}

	private void preparationJeu()
	{
		classement = new int[list_joueur.size()];

		ressource_joueur_fini = new tabRessource[list_joueur.size()];

		int i;
		for(i=0; i<ressource_joueur_fini.length;i++)
		{
			ressource_joueur_fini[i]=new tabRessource();
		}

		sendList();

		lancementJeu();

		if(RbR)
		{
			boucleDeTour();
		}
	}


	public static void main(String args[])
	{
		if (args.length != 7)
		{
			System.out.println("Usage : java ServeurChatImpl" + " <machineServeurDeNoms>" + " <No Port>" + " <mode de jeu: L/R> " + "<mode de fin: F/A>"+ " mode d'évaluation: P/R "+ " nb de joueur" + " nb de prod" ) ;
			return ;
		}
		try
		{
			String [] argv = {"-ORBInitialHost", args[0], "-ORBInitialPort", args[1]} ; 
			ORB orb = ORB.init(argv, null) ;
			CoordinateurImpl coord = new CoordinateurImpl(args[2],args[3],args[4],Integer.parseInt(args[5]),Integer.parseInt(args[6])) ;

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

			System.out.println("Préparation du jeu");

			coord.preparationJeu();


			coord.lock.lock();
			try{
				coord.terminaison.await();
			} finally {
				coord.lock.unlock();
			}

			System.out.println("Fin du jeu");

			int i;

			for(i=0;i<coord.classement.length;i++)
			{
				System.out.print((i+1)+") "+coord.classement[i]);
				if(coord.modeEval == 0)
				{
					System.out.print(" Nb ressource "+ coord.nbRessourceParJoueur.get(new Integer(i+1)));
				}
				System.out.print("\n");
			}





		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e) ;
			e.printStackTrace(System.out) ;
		}
		finally {
			System.out.println("Coordinateur Exiting ...") ;
			System.exit(0);
		}
	}
}
