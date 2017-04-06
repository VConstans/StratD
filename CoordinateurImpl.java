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

	Vector<Joueur> list_joueur = new Vector<Joueur>();
	Vector<Producteur> list_prod = new Vector<Producteur>();

	int maxJoueur=5;
	int maxProd=5;

	public boolean ajoutJoueur(Joueur j)
	{
		if(list_joueur.size()<=maxJoueur)
		{
			list_joueur.addElement(j);
			return true;
		}
		else
		{
			return false;
		}
	}

	public boolean ajoutProd(Producteur p)
	{
		if(list_joueur.size()<=maxProd)
		{
			list_prod.addElement(p);
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
			orb.run() ;
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e) ;
			e.printStackTrace(System.out) ;
		}
      
		System.out.println("Coordinateur Exiting ...") ;
	}
}
