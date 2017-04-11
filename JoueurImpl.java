import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
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
	Coordinateur cord;
	ThreadRun thread;
	int id;

	Producteur[] list_prod;

	int[] ressource=new int[5];

	private void demandeRessource(int p,int r,int n)
	{
		Ressource re = new Ressource(r,n);
		if(list_prod[p].demandeRessource(re))
		{
			ressource[r]+=n;
		}
	}

	private void connection()
	{
		id = cord.ajoutJoueur(player);
		if(id != -1)
		{
			cord.ping(id);
		}
		else
		{
			System.out.println("Erreur connection "+id);
		}
	}


	public void gameLoop()
	{
		demandeRessource(0,1,5);
	}

	public void annonce()
	{
		System.out.println("Joueur : "+id);
	}


	public void rcvListProd(Producteur[] prod)
	{
		list_prod = prod;
		System.out.println(id+" : "+list_prod.length);
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
			joueur.cord = CoordinateurHelper.narrow(obj) ;
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
