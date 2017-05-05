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


	String[] connaissanceRessource;

	boolean mon_tour = true;

	boolean protege = false;

	boolean RbR = false;

	boolean humain =false;

	boolean fini = false;

	Lock tour = new ReentrantLock();
	Condition entrerTour = tour.newCondition();
	Condition finTour = tour.newCondition();

	Lock lock = new ReentrantLock();
	Condition terminaison = lock.newCondition();
	

	public JoueurImpl(String p)
	{
		if(p.equals("H"))
		{
			humain = true;
		}
	}


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
		if(fini)
		{
			return 0;
		}

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
		connaissanceRessource = new String[list_prod.length];


		list_joueur = joueur;


		ressourceEnJeu = ressource;

		int i;


		for(i=0;i<tabBesoin.length;i++)
		{
			besoin.put(tabBesoin[i].type,tabBesoin[i].nb);
		}

		this.RbR = RbR;

		if(humain && !RbR)
		{
			System.out.println("Erreur, un joueur humain est présent, le jeu doit être en tour par tour");
		}

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
		//TODO supprimer methode et appeler direct apprentissage si elle reste vide
		apprentissageRessource(idProd,r);
	}


	public void arretJoueur()
	{
		//TODO inutile si juste un appel
		finPartie();
	}


	synchronized private void penaliseVole(int transaction)
	{
		for(Map.Entry<String,Integer> entree : ((ressource.getTab()).entrySet()))
		{
			ressource.put(entree.getKey(),entree.getValue()/2);
		}

		listTransaction.get(transaction).penalise = true;
		System.out.println("penalisation vole");
	}


	public void terminaison()
	{
		lock.lock();
		try{
			terminaison.signal();
		} finally {
			lock.unlock();
		}
	}


	public void gameLoop()
	{

//		commenceObservation();


		while(!verifRessource() && !fini)
		{
			if(RbR)
			{
				prendTour();
			}


/*			if(humain)
			{
				try {
					commandeHumain();
				} catch (Exception e) {System.out.println("erreur");}
			}
			else
			{

				String ressourceCritique = ressourceARechercher();
				int prodTrouver = rechercheProducteur(ressourceCritique);
				int qte;

				if(prodTrouver == -1)
				{
					int prod_a_sonder;
					Ressource ressource_prod_sonder;

					prod_a_sonder = choixProdSonder();
					ressource_prod_sonder=sondeProd(prod_a_sonder);

					while(!ressourceCritique.equals(ressource_prod_sonder.type));
					{
						
						if(RbR)
						{
							coord.finTour();
							prendTour();
						}

						prod_a_sonder = choixProdSonder();
						ressource_prod_sonder=sondeProd(prod_a_sonder);

					}

					apprentissageRessource(prod_a_sonder,ressource_prod_sonder);

					

					prodTrouver = prod_a_sonder;
					qte = ressource_prod_sonder.nb;
				}
				else
				{
					Ressource ressource_prod_sonder=sondeProd(prodTrouver);
					qte = ressource_prod_sonder.nb;
				}

				if(RbR)
				{
					coord.finTour();
					prendTour();
				}
				demandeRessource(prodTrouver,new Ressource(ressourceCritique,qte));
*/
				demandeRessource(1,new Ressource("petrole",1));
/*
			}
			if(RbR)
			{
				coord.finTour();
			}
*/
		}
//		finObservation();

		finPartie();
	}


	private void commandeHumain() throws IOException
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String commande = null;
		boolean commande_valide = false;
	 
		while (!commande_valide)
		{
			commande = in.readLine();
			String[] args = commande.split(" ");
	 
			switch (args[0])
			{
				case "demande":
					System.out.println("Prend");
					commande_valide = true;
			}
		}
	}


	private int choixProdSonder()
	{
		int max = list_prod.length -1;
		int genere;

		do{
			genere=(int)(Math.random()*(float)max);
		}while(connaissanceRessource[genere] != null && !connaissanceRessource[genere].isEmpty());

		return genere+1;
	}


	private int rechercheProducteur(String ressourceRechercher)
	{
		int i;
		for(i=0 ; i<connaissanceRessource.length ; i++)
		{
			if(ressourceRechercher.equals(connaissanceRessource[i]))
			{
				return i+1;
			}
		}
		return -1;
	}

	private String ressourceARechercher()
	{
		int max = 0;
		String recherche = null;

		for(String r : ressourceEnJeu)
		{
			if(besoin.containsKey(r))
			{
				int nbRessource;

				if(ressource.containsKey(r))
				{
					nbRessource = ressource.get(r);
				}
				else
				{
					nbRessource = 0;
				}
				int delta = besoin.get(r) - nbRessource;

				if(delta > max)
				{
					max = delta;
					recherche = r;
				}
			}
		}

		return recherche;
	}


	private void finPartie()
	{
		if(!fini)
		{
			fini = true;
			Transaction[] transacTmp= new Transaction[listTransaction.size()];
			transacTmp = listTransaction.toArray(transacTmp);
			coord.finJoueur(id, transacTmp);
			envoieRessource();
			stopJoueur();
		}
	}


	private void stopJoueur()
	{
		coord.signalJoueurStopper();
		//TODO arret orb et fin programme
	}


	private void envoieRessource()
	{
		for(Map.Entry<String,Integer> entree : ((ressource.getTab()).entrySet()))
		{
			coord.recuperationRessourceJoueur(id,entree.getKey(),entree.getValue());
		}
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


	private void apprentissageRessource(int id, Ressource r)
	{
			connaissanceRessource[id-1]=r.type;
	}

	synchronized private void demandeRessource(int p,Ressource r)
	{
		if(r.nb < 0) System.out.println("=================================================================================================>");

		if(list_prod[p-1].demandeRessource(r))
		{
			System.out.println("demande accordé");

			if(!ressource.containsKey(r.type))
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
//			System.out.println("Demande impossible");
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
//		System.out.println("=================================");
		int i;
		for(Map.Entry<String,Integer> entree : ((besoin.getTab()).entrySet()))
		{
//			System.out.println("Besoin "+entree.getValue()+" Possede "+ressource.get(entree.getKey()));
			
			if(ressource.get(entree.getKey()) < entree.getValue())
			{
				return false;
			}
		}

		return true;
	}


	private Ressource sondeProd(int idProd)
	{
		return list_prod[idProd-1].sondeProd();
	}




	public static void main(String args[])
	{
		JoueurImpl joueur = null ;

		if (args.length != 3)
		{
			System.out.println("Usage : java JoueurImpl" + " <machineServeurDeNoms>" + " <No Port>"+ "<Humain: H/M>") ;
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
			joueur = new JoueurImpl(args[2]) ;


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
			if (joueur.coord == null)
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

			//joueur.thread.join();
			joueur.lock.lock();
			try{
				joueur.terminaison.await();
			} finally {
			joueur.lock.unlock();
			}

			System.out.println("Fin");
			
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
	//		if (joueur != null)
		//	joueur.thread.shutdown() ;
			System.exit(0);
		}
	}
	
}
