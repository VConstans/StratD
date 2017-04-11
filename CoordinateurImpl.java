import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import java.util.*;
import org.omg.CosNaming.*;
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


	int maxJoueur=3;
	int maxProd=3;

	synchronized public int ajoutJoueur(Joueur j)
	{
		if(list_joueur.size()<maxJoueur)
		{
			list_joueur.add(j);
			System.out.println("======+>"+list_joueur.size());
	//		sendList();
			return list_joueur.size();
		}
		else
		{
			return -1;
		}
	}

	synchronized public int ajoutProd(Producteur p)
	{
		if(list_prod.size()<maxProd)
		{
			list_prod.add(p);
//			System.out.println(list_prod.size());
	//		sendList();
			return list_prod.size();
		}
		else
		{
			return -1;
		}
	}

	public void ping(int id)
	{
		System.out.println("Conection "+id);
	}


	private void sendList()
	{
//		if(list_joueur.size()==maxJoueur && list_joueur.size() == maxProd)
//		{
			Producteur[] tabProd =new Producteur[list_prod.size()];
			tabProd = list_prod.toArray(tabProd);

			int i,j;
			
			System.out.println("=============================>"+list_joueur.size());
			for(i=0;i<list_joueur.size();i++)
			{
				System.out.println("=============+>boucle");
				list_joueur.get(i).rcvListProd(tabProd);
			}
//		}
	}


	private void lancementJeu()
	{
		int i;
		for(i=0;i<list_joueur.size();i++)
		{
			list_joueur.get(i).gameLoop();
		}
	}

	private void preparationJeu()
	{
		System.out.println("===================================+>passe");
		sendList();

		lancementJeu();
	}


	public static void main(String args[])
	{
		if (args.length != 2)
		{
			System.out.println("Usage : java ServeurChatImpl" + " <machineServeurDeNoms>" + " <No Port>") ;
			return ;
		}
		try
		{
			String [] argv = {"-ORBInitialHost", args[0], "-ORBInitialPort", args[1]} ; 
			ORB orb = ORB.init(argv, null) ;
			CoordinateurImpl coord = new CoordinateurImpl() ;

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

			Thread.sleep(10000);



			System.out.println("après");
			System.out.flush();

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
