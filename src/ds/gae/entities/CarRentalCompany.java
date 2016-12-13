package ds.gae.entities;

import javax.persistence.*;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.datanucleus.annotations.Unowned;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import ds.gae.EMF;
import ds.gae.ReservationException;

@Entity
@NamedQueries({ @NamedQuery(name = "Rental.FindAll", query = "SELECT com FROM CarRentalCompany com"),
		@NamedQuery(name = "Rental.FindAllCompanyNames", query = "SELECT com.name FROM CarRentalCompany com"),
		@NamedQuery(name = "Rental.FindAllCarTypesForCompany", query = "SELECT com.carTypes FROM CarRentalCompany com WHERE com.key = :company") })
public class CarRentalCompany {

	private static Logger logger = Logger.getLogger(CarRentalCompany.class.getName());

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key key;
	@Basic
	private String name;
	@OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
	private Set<CarType> carTypes = new HashSet<CarType>();

	/***************
	 * CONSTRUCTOR *
	 ***************/
	public CarRentalCompany() {

	}

	public CarRentalCompany(String name, Set<CarType> types) {
		logger.log(Level.INFO, "<{0}> Car Rental Company {0} starting up...", name);
		setName(name);
		setCarTypes(types);

	}

	public Key getKey() {
		return key;
	}

	/********
	 * NAME *
	 ********/

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/*************
	 * CAR TYPES *
	 *************/

	public void setCarTypes(Set<CarType> types) {
		this.carTypes = types;
	}

	public void addCarType(CarType type) {
		carTypes.add(type);
	}

	public Collection<CarType> getAllCarTypes() {
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("Rental.FindAllCarTypesForCompany", Set.class);
		Set<CarType> typeSet = new HashSet<CarType>();
		try {
			query.setParameter("company", this.getKey());
			List<Set<CarType>> typeSets = query.getResultList();
			if (!typeSets.isEmpty()) {
				typeSet = typeSets.get(0);
			}
			return typeSet;
		} finally {
			em.close();
		}
	}

	public CarType getCarType(String carTypeName) {
		for (CarType type : getAllCarTypes()) {
			if (type.getName().equals(carTypeName)) {
				return type;
			}
		}
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}

	public boolean isAvailable(String carTypeName, Date start, Date end) {
		logger.log(Level.INFO, "<{0}> Checking availability for car type {1}", new Object[] { name, carTypeName });
		for (CarType type : getAllCarTypes()) {
			if (type.getName().equals(carTypeName)) {
				return getAvailableCarTypes(start, end).contains(type);
			}
		}
		throw new IllegalArgumentException("<" + carTypeName + "> No car type of name " + carTypeName);
	}

	public Set<CarType> getAvailableCarTypes(Date start, Date end) {
		Set<CarType> availableCarTypes = new HashSet<CarType>();
		for (Car car : getCarsQuery()) {
			if (car.isAvailable(start, end)) {
				availableCarTypes.add(car.getType());
			}
		}
		return availableCarTypes;
	}

	/*********
	 * CARS *
	 *********/

	private Car getCar(int uid) {
		for (Car car : getCarsQuery()) {
			if (car.getId() == uid)
				return car;
		}
		throw new IllegalArgumentException("<" + name + "> No car with uid " + uid);
	}

	public Set<Car> getCarsQuery() {
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("CarType.FindAllCarsForType", Set.class);
		Set<Car> carSet = new HashSet<Car>();
		try {
			for (CarType type : getAllCarTypes()) {
				query.setParameter("typeKey", type.getKey());
				List<Set<Car>> carSets = query.getResultList();
				if (!carSets.isEmpty()) {
					carSet.addAll(carSets.get(0));
				}
			}
			return carSet;
		} finally {
			em.close();
		}
	}

	private List<Car> getAvailableCars(String carType, Date start, Date end) {
		List<Car> availableCars = new LinkedList<Car>();
		for (Car car : getCarsQuery()) {
			if (car.getType().getName().equals(carType) && car.isAvailable(start, end)) {
				availableCars.add(car);
			}
		}
		return availableCars;
	}

	/****************
	 * RESERVATIONS *
	 ****************/

	public Quote createQuote(ReservationConstraints constraints, String client) throws ReservationException {
		logger.log(Level.INFO, "<{0}> Creating tentative reservation for {1} with constraints {2}",
				new Object[] { name, client, constraints.toString() });

		CarType type = getCarType(constraints.getCarType());

		if (!isAvailable(constraints.getCarType(), constraints.getStartDate(), constraints.getEndDate()))
			throw new ReservationException("<" + name + "> No cars available to satisfy the given constraints.");

		double price = calculateRentalPrice(type.getRentalPricePerDay(), constraints.getStartDate(),
				constraints.getEndDate());

		return new Quote(client, constraints.getStartDate(), constraints.getEndDate(), getName(),
				constraints.getCarType(), price);
	}

	// Implementation can be subject to different pricing strategies
	private double calculateRentalPrice(double rentalPricePerDay, Date start, Date end) {
		return rentalPricePerDay * Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24D));
	}

	public Reservation confirmQuote(Quote quote) throws ReservationException {
		logger.log(Level.INFO, "<{0}> Reservation of {1}", new Object[] { name, quote.toString() });
		List<Car> availableCars = getAvailableCars(quote.getCarType(), quote.getStartDate(), quote.getEndDate());
		if (availableCars.isEmpty())
			throw new ReservationException("Reservation failed, all cars of type " + quote.getCarType()
					+ " are unavailable from " + quote.getStartDate() + " to " + quote.getEndDate());
		Car car = availableCars.get((int) (Math.random() * availableCars.size()));
		Reservation res = new Reservation(quote, car);
		car.addReservation(res);

		return res;
	}

	public void cancelReservation(Reservation res) {
		logger.log(Level.INFO, "<{0}> Cancelling reservation {1}", new Object[] { name, res.toString() });
		getCar(res.getCar().getId()).removeReservation(res);
	}
}