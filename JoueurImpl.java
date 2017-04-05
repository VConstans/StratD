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


public class JoueurImpl extends JoueurPOA
{
	Joueur player;
	Coordinateur cord;
	ThreadRun thread;


	public boolean demandeRessource(int n)
	{
		return true;	//TODO renvoyer bonne valeur
	}

	public void annonce()
	{
		System.out.println("Joueur");
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
			joueur.cord.ping();
			joueur.cord.ajoutJoueur(joueur.player);
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
			if (prod != null)
			joueur.thread.shutdown() ;
		}
	}
	
}
