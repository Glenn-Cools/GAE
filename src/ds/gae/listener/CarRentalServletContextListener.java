package ds.gae.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ds.gae.CarRentalModel;
import ds.gae.EMF;
import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;

public class CarRentalServletContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// This will be invoked as part of a warming request,
		// or the first user request if no warming request was invoked.

		// check if dummy data is available, and add if necessary
		if (!isDummyDataAvailable()) {
			addDummyData();
		}
	}

	private boolean isDummyDataAvailable() {
		// If the Hertz car rental company is in the datastore, we assume the
		// dummy data is available

		//  use persistence instead
		//return CarRentalModel.get().CRCS.containsKey("Hertz");
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query query = em.createNamedQuery("Rental.FindAll", CarRentalCompany.class);
			boolean available = !query.getResultList().isEmpty();
			return available;
		} finally {
			em.close();
		}

	}

	private void addDummyData() {
		loadRental("Hertz", "hertz.csv");
		loadRental("Dockx", "dockx.csv");
	}

	private void loadRental(String name, String datafile) {
		Logger.getLogger(CarRentalServletContextListener.class.getName()).log(Level.INFO, "loading {0} from file {1}",
				new Object[] { name, datafile });
		EntityManager em = EMF.get().createEntityManager();
		try {

			CarRentalCompany company = loadData(name, datafile);
			em.persist(company);

		} catch (NumberFormatException ex) {
			Logger.getLogger(CarRentalServletContextListener.class.getName()).log(Level.SEVERE, "bad file", ex);
		} catch (IOException ex) {
			Logger.getLogger(CarRentalServletContextListener.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (em != null) {
				em.close();
			}
		}
	}


	public static CarRentalCompany loadData(String name, String datafile) throws NumberFormatException, IOException {

		CarRentalCompany company = new CarRentalCompany();
		company.setName(name);
		
		//leave it cuz it's needed to create new cars
		int carId = 1;

		// open file from jar
		BufferedReader in = new BufferedReader(new InputStreamReader(
				CarRentalServletContextListener.class.getClassLoader().getResourceAsStream(datafile)));
		// while next line exists
		while (in.ready()) {
			// read line
			String line = in.readLine();
			// if comment: skip
			if (line.startsWith("#")) {
				continue;
			}
			// tokenize on ,
			StringTokenizer csvReader = new StringTokenizer(line, ",");
			// create new car type from first 5 fields
			CarType type = new CarType(csvReader.nextToken(), Integer.parseInt(csvReader.nextToken()),
					Float.parseFloat(csvReader.nextToken()), Double.parseDouble(csvReader.nextToken()),
					Boolean.parseBoolean(csvReader.nextToken()), company);
			// create N new cars with given type, where N is the 5th field
			for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
				type.addCar(new Car(carId++, type));
			}
			company.addCarType(type);
			
		}

		return company;
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// App Engine does not currently invoke this method.
	}
}