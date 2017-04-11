import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
import java.io.*;
import java.io.IOException;
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
	Coordinateur cord;
	ThreadRun thread;

	int ressourceType;
	int nbressource;
	int produit;

	public ProducteurImpl(int type,int nb)
	{
		ressourceType=type;
		nbressource=nb;
		produit=10;
	}


	public boolean demandeRessource(Ressource r)
	{
		if(r.type == ressourceType && r.nb <= produit)
		{
			produit-=r.nb;
			System.out.println("ressource apres demande "+produit);
			return true;
		}
		else
		{
			return false;
		}
	}

	public void annonce()
	{
		System.out.println("Prod");
	}

	public static void main(String args[])
	{
		ProducteurImpl prod = null ;

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
			prod = new ProducteurImpl(1,10) ;	//TODO changer parametre constructeur
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
			prod.cord = CoordinateurHelper.narrow(obj) ;
			if (prod.producteur == null)
			{
				System.out.println("Pb pour contacter le serveur") ;
				System.exit(1) ;
			}
			else
			//	System.out.println("Annonce du serveur : " + client.serveur.ping()) ;

			// lancer l'ORB dans un thread
			prod.thread = new ThreadRun(orb) ;
			prod.thread.start() ;
		//	orb.run();
			prod.cord.ping(1000);
			prod.cord.ajoutProd(prod.producteur);
			prod.thread.join();
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
			prod.thread.shutdown() ;
		}
	}
	
}
