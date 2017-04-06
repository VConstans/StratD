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


	int maxJoueur=2;
	int maxProd=2;

	public boolean ajoutJoueur(Joueur j)
	{
		if(list_joueur.size()<maxJoueur)
		{
			list_joueur.add(j);
	//		sendList();
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean ajoutProd(Producteur p)
	{
		if(list_prod.size()<maxProd)
		{
			list_prod.add(p);
	//		sendList();
			return true;
		}
		else
		{
			return false;
		}
	}

	public void ping()
	{
		System.out.println("Conection");
	}


	private void sendList()
	{
		if(list_joueur.size()==maxJoueur && list_joueur.size() == maxProd)
		{
		int i,j;
		for(i=0;i<list_joueur.size();i++)
		{
			for(j=0;j<list_prod.size();j++)
			{
				list_joueur.get(i).rcvListProd(list_prod.get(j));
			
			}
		}
		}
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

			while(coord.list_joueur.size()<coord.maxJoueur && coord.list_joueur.size()<coord.maxProd);
			System.out.println("après");
			coord.sendList();

//			coord.startGame();
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e) ;
			e.printStackTrace(System.out) ;
		}
      
		System.out.println("Coordinateur Exiting ...") ;
	}
}
