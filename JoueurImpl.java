import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.io.*;
import java.io.IOException;
import java.sql.*;
import org.omg.CosNaming.*;
import StratD.Coordinateur;
import StratD.CoordinateurHelper;
import StratD.Joueur;
import StratD.JoueurPOA;
import StratD.JoueurHelper;
import StratD.Producteur;
import StratD.Ressource;
import StratD.Transaction;


public class JoueurImpl extends JoueurPOA
{
	Joueur player;
	Coordinateur coord;
	ThreadRun thread;
	int id;

	Producteur[] list_prod;
	Joueur[] list_joueur;
	ArrayList<Joueur> observateur = new ArrayList<Joueur>();
	ArrayList<Transaction> listTransaction = new ArrayList<Transaction>();
	tabRessource ressource = new tabRessource();
	tabRessource besoin = new tabRessource();

	String[] ressourceEnJeu;


	Ressource[] connaissanceRessource;

	boolean mon_tour = true;

	boolean protege = false;

	boolean RbR = false;

	Lock tour = new ReentrantLock();
	Condition entrerTour = tour.newCondition();
	Condition finTour = tour.newCondition();
	


	public void donneTour()// throws InterruptedException
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


	synchronized public int estVole(Ressource r)
	{
		if(protege)
		{
			System.out.println("Voleur vu");
			return -1;
		}
		else
		{
			System.out.println(id+"tu es volé");
			int nbRessourceCourrante = ressource.get(r.type);
			if(nbRessourceCourrante == -1)
			{
				return 0;
			}

			if(r.nb <= nbRessourceCourrante)
			{
				ressource.put(r.type,nbRessourceCourrante-r.nb);
				return 1;

			}
			else
			{
				return 0;
			}
		}
	}


	public void rcvParametreJeu(Joueur[] joueur, Producteur[] prod, String[] ressource, Ressource[] tabBesoin, boolean RbR)
	{
		list_prod = prod;
		connaissanceRessource = new Ressource[list_prod.length];


		list_joueur = joueur;


		ressourceEnJeu = ressource;

		int i;

		for(i=0;i<tabBesoin.length;i++)
		{
			besoin.put(tabBesoin[i].type,tabBesoin[i].nb);
		}

		this.RbR = RbR;

	}


	public void ajoutObservateur(Joueur j)
	{
		
		//System.out.println(id+") ajoute obs");
		observateur.add(j);
	}

	public void suppObservateur(Joueur j)
	{
		//System.out.println(id+") supp obs");
		if(!observateur.remove(j))
		{
			//System.out.println("Erreur suppression observateur");
		}
	}


	public void observe(int idProd,Ressource r)
	{
		//TODO if observe
		//TODO supprimer methode et appeler direct apprentissage si elle reste vide
		apprentissageRessource(idProd,r);
	}


	public void finDePartie()
	{
		System.out.println("Fin de partie");
	}


	synchronized private void penaliseVole(int transaction)
	{
		listTransaction.get(transaction).penalise = true;
		System.out.println("penalisation vole");
	}


	public void gameLoop()
	{
		//System.out.println("Game loop");

		commenceObservation();


		while(!verifRessource())
		{
			if(RbR)
			{
				prendTour();
			}

			demandeRessource(0,new Ressource("petrole",1));

			if(RbR)
			{
				coord.finTour();
			}
		}
		finObservation();

		coord.finJoueur(id);

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


	private void commenceObservation()
	{
		
		//System.out.println(id+") observe");
		int i;

		for(i=0;i<list_joueur.length;i++)
		{
			if(id != i+1)
				list_joueur[i].ajoutObservateur(player);
		}
	}


	private void finObservation()
	{
		//System.out.println(id+") arrete observe");
		int i;

		for(i=0;i<list_joueur.length;i++)
		{
			if(id != i+1)
				list_joueur[i].suppObservateur(player);
		}
	}


	private void apprentissageRessource(int p, Ressource r)
	{
		if(connaissanceRessource[p] == null || connaissanceRessource[p].type != r.type)
		{
			connaissanceRessource[p]=r;
		}
	}

	synchronized private void demandeRessource(int p,Ressource r)
	{
		if(list_prod[p].demandeRessource(r))
		{

			if(ressource.get(r.type) == -1)
			{
				ressource.put(r.type,r.nb);
			}
			else
			{
				ressource.put(r.type,ressource.get(r.type)+r.nb);
			}
			apprentissageRessource(p,r);
			Timestamp time = new Timestamp(System.currentTimeMillis());
			listTransaction.add(new Transaction(time.getTime(),id,p,r,false,false));
		}
		else
		{
			System.out.println("Demande impossible");
		}
	}


	private void annonceObservation(int idProd,Ressource r)
	{
		int i;

		for(i=0;i<observateur.size();i++)
		{
			observateur.get(i).observe(idProd,r);
		}
	}


	synchronized private void vole(int j,Ressource r)
	{
		Timestamp time = new Timestamp(System.currentTimeMillis());
		listTransaction.add(new Transaction(time.getTime(),id,j,r,true,false));

		switch(list_joueur[j].estVole(r))
		{
			case 1:
				//System.out.println(id+") ressource "+r.type+" avant vole "+ressource[r.type]);

				System.out.println(id+" vole");
				if(ressource.get(r.type) == -1)
				{
					ressource.put(r.type,r.nb);
				}
				else
				{
					ressource.put(r.type,ressource.get(r.type)+r.nb);
				}
					//System.out.println(id+") ressource après vole "+ressource[r.type]);
				//	annonceVole(listTransaction.size()-1);

				break;
			case -1:
				penaliseVole(listTransaction.size()-1);
				break;
			case 0:
	//			System.out.println("Demande impossible");
				break;
		}
	}


	private void connection()
	{
		id = coord.ajoutJoueur(player);

		if(id == -1)
		{
				System.out.println("Plus de place disponible "+id);
		}
		else
		{
				coord.ping(id);
		}
	}


	private boolean verifRessource()
	{
		int i;
		for(Map.Entry<String,Integer> entree : ((besoin.getTab()).entrySet()))
		{
			if(ressource.get(entree.getKey()) < entree.getValue())
			{
				return false;
			}
		}

		return true;
	}


	private void sondeProd(int idProd)
	{
		apprentissageRessource(idProd,list_prod[idProd].sondeProd());
	}




	public static void main(String args[])
	{
		JoueurImpl joueur = null ;

		if (args.length != 2)
		{
			System.out.println("Usage : java ClientChatImpl" + " <machineServeurDeNoms>" + " <No Port>") ;
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
			joueur = new JoueurImpl() ;

			joueur.besoin.put("petrole",7);	//TODO Enlever
			joueur.ressource.put("petrole",3);	//TODO Enlever

			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(joueur) ;
			joueur.player = JoueurHelper.narrow(ref) ; 
			if (joueur == null)
			{
				System.out.println("Pb pour obtenir une ref sur le client") ;
				System.exit(1) ;
			}

			// contacter le serveur
			String reference = "corbaname::" + args[0] + ":" + args[1] + "#Coordinateur" ;
			org.omg.CORBA.Object obj = orb.string_to_object(reference) ;

			// obtenir reference sur l'objet distant
			joueur.coord = CoordinateurHelper.narrow(obj) ;
			if (joueur.player == null)
			{
				System.out.println("Pb pour contacter le serveur") ;
				System.exit(1) ;
			}
			else
			//	System.out.println("Annonce du serveur : " + client.serveur.ping()) ;

			// lancer l'ORB dans un thread
			joueur.thread = new ThreadRun(orb) ;
			joueur.thread.start() ;

			joueur.connection();

			joueur.thread.join();
		//	prod.loop() ;
		}
		catch (Exception e)
		{
			System.out.println("ERROR : " + e) ;
			e.printStackTrace(System.out) ;
		}
		finally
		{
			// shutdown
			if (joueur != null)
			joueur.thread.shutdown() ;
		}
	}
	
}
