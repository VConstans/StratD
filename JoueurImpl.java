import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.io.*;
import java.io.IOException;
import org.omg.CosNaming.*;
import StratD.Coordinateur;
import StratD.CoordinateurHelper;
import StratD.Joueur;
import StratD.JoueurPOA;
import StratD.JoueurHelper;
import StratD.Producteur;
import StratD.Ressource;


public class JoueurImpl extends JoueurPOA
{
	Joueur player;
	Coordinateur coord;
	ThreadRun thread;
	int id;

	Producteur[] list_prod;
	Joueur[] list_joueur;
	ArrayList<Joueur> observateur = new ArrayList<Joueur>();

	Ressource[] connaissanceRessource;

	int[] ressource=new int[5];
	int[] besoin=new int[5];


	boolean RbR = false;	//TODO modifier

	Lock tour = new ReentrantLock();


	public JoueurImpl(String[] args)
	{
		if(args[0].equals("R"))
		{
			RbR=true;
		}

		tour.lock();
	}


	public void joueTour()
	{
		try
		{
		tour.unlock();
		} catch (Exception e)
		{
			System.out.println("==========+++>erreur "+id);
		}
	}


	synchronized public boolean estVole(Ressource r)
	{
		if(r.nb <= ressource[r.type])
		{
			ressource[r.type]-=r.nb;
			return true;
		}
		else
		{
			return false;
		}
	}



	public void rcvListProd(Producteur[] prod)
	{
		list_prod = prod;
		connaissanceRessource = new Ressource[list_prod.length];
		System.out.println(id+" : "+list_prod.length);
	}


	public void rcvListJoueur(Joueur[] joueur)
	{
		list_joueur = joueur;
	}


	public void ajoutObservateur(Joueur j)
	{
		observateur.add(j);
	}

	public void suppObservateur(Joueur j)
	{
		if(!observateur.remove(j))
		{
			System.out.println("Erreur suppression observateur");
		}
	}


	public void observeVole(int i)
	{
		System.out.println("Vole commis par "+i+" observé par "+id);
	}


	public void gameLoop()
	{

		commenceObservation();
		while(!verifRessource())
		{
			if(RbR)
			{
				tour.lock();
			}
			//demandeRessource(0,new Ressource(0,1));
			if(id!=1)
				vole(0,new Ressource(0,1));
			if(RbR)
			{
			//	coord.finTour();
			}
		}
		finObservation();
	}


	private void commenceObservation()
	{
		int i;

		for(i=0;i<list_joueur.length;i++)
		{
			list_joueur[i].ajoutObservateur(player);
		}
	}


	private void finObservation()
	{
		int i;

		for(i=0;i<list_joueur.length;i++)
		{
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
			System.out.println(id+") ressource avant demande "+ressource[r.type]);
			ressource[r.type]+=r.nb;
			System.out.println(id+") ressource après demande "+ressource[r.type]);
			apprentissageRessource(p,r);
		}
		else
		{
//			System.out.println("Demande impossible");
		}
	}


	private void annonceVole()
	{
		int i;

		for(i=0;i<observateur.size();i++)
		{
			observateur.get(i).observeVole(id);
		}
	}


	synchronized private void vole(int j,Ressource r)
	{

		if(list_joueur[j].estVole(r))
		{
			System.out.println(id+") ressource "+r.type+" avant vole "+ressource[r.type]);
			ressource[r.type]+=r.nb;
			System.out.println(id+") ressource après vole "+ressource[r.type]);
			annonceVole();
		}
		else
		{
//			System.out.println("Demande impossible");
		}
	}


	private void connection()
	{
		id = coord.ajoutJoueur(player, RbR);

		switch(id)
		{
			case -1:
				System.out.println("Plus de place disponible "+id);
				break;
			case -2:
				System.out.println("Mode de jeu incompatible "+id);
				break;
			default:
				coord.ping(id);
				break;

		}
	}


	private boolean verifRessource()
	{
		int i;
		for(i=0;i<ressource.length;i++)
		{
			if(ressource[i] < besoin[i])
			{
				return false;
			}
		}

		return true;
	}





	public static void main(String args[])
	{
		JoueurImpl joueur = null ;

		if (args.length != 3)
		{
			System.out.println("Usage : java ClientChatImpl" + " <machineServeurDeNoms>" + " <No Port>" + " <R>") ;
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
			joueur = new JoueurImpl(args) ;

			joueur.besoin[0]=7;	//TODO Enlever
			joueur.ressource[0]=5;	//TODO Enlever

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
