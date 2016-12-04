package ds.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;

public class CarRentalModel {

	public Map<String, CarRentalCompany> CRCS = new HashMap<String, CarRentalCompany>();

	private static CarRentalModel instance;

	public static CarRentalModel get() {
		if (instance == null)
			instance = new CarRentalModel();
		return instance;
	}

	private List<CarRentalCompany> getAllRentals() {
		List<CarRentalCompany> crcs = null;
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query query = em.createNamedQuery("Rental.FindAll", CarRentalCompany.class);
			crcs = query.getResultList();
		} finally {
			em.close();
		}
		return crcs;
	}

	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param crcName
	 *            the car rental company
	 * @return The list of car types (i.e. name of car type), available in the
	 *         given car rental company.
	 */
	public Set<String> getCarTypesNames(String crcName) {
		Set<String> typeNames = new HashSet<String>();
		EntityManager em = EMF.get().createEntityManager();
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(crcName)) {
					Query query = em.createNamedQuery("Rental.FindAllCarTypesForCompany", CarType.class);
					query.setParameter("company", crc.getKey());
					List<CarType> types =  query.getResultList();
					for (CarType type: types){
						typeNames.add(type.getName());
					}
					return typeNames;
				}
			}

			return typeNames;
		} finally {
			em.close();
		}
		
	}

	/**
	 * Get all registered car rental companies
	 *
	 * @return the list of car rental companies
	 */
	public Collection<String> getAllRentalCompanyNames() {
		// use persistence instead
		// return CRCS.keySet();

		EntityManager em = EMF.get().createEntityManager();
		try {
			Query query = em.createNamedQuery("Rental.FindAllCompanyNames", String.class);
			return query.getResultList();
		} finally {
			em.close();
		}
	}

	/**
	 * Create a quote according to the given reservation constraints (tentative
	 * reservation).
	 * 
	 * @param company
	 *            name of the car renter company
	 * @param renterName
	 *            name of the car renter
	 * @param constraints
	 *            reservation constraints for the quote
	 * @return The newly created quote.
	 * 
	 * @throws ReservationException
	 *             No car available that fits the given constraints.
	 */
	public Quote createQuote(String company, String renterName, ReservationConstraints constraints)
			throws ReservationException {
		//  use persistence instead

		/*
		 * CarRentalCompany crc = CRCS.get(company); Quote out = null;
		 * 
		 * if (crc != null) { out = crc.createQuote(constraints, renterName); }
		 * else { throw new ReservationException("CarRentalCompany not found.");
		 * }
		 * 
		 * return out;
		 */

		Quote out = null;
		for (CarRentalCompany crc : getAllRentals()) {
			if (crc.getName().equals(company)) {
				out = crc.createQuote(constraints, renterName);
				return out;
			}
		}

		throw new ReservationException("CarRentalCompany not found.");

	}

	/**
	 * Confirm the given quote.
	 *
	 * @param q
	 *            Quote to confirm
	 * 
	 * @throws ReservationException
	 *             Confirmation of given quote failed.
	 */
	public synchronized Reservation confirmQuote(Quote q) throws ReservationException {
		// use persistence instead

		/*
		 * CarRentalCompany crc = CRCS.get(q.getRentalCompany());
		 * crc.confirmQuote(q);
		 */

		for (CarRentalCompany crc : getAllRentals()) {
			if (crc.getName().equals(q.getRentalCompany())) {
				return crc.confirmQuote(q);
			}
		}

		throw new ReservationException("CarRentalCompany not found.");
	}

	/**
	 * Confirm the given list of quotes
	 * 
	 * @param quotes
	 *            the quotes to confirm
	 * @return The list of reservations, resulting from confirming all given
	 *         quotes.
	 * 
	 * @throws ReservationException
	 *             One of the quotes cannot be confirmed. Therefore none of the
	 *             given quotes is confirmed.
	 */
	public List<Reservation> confirmQuotes(List<Quote> quotes) throws ReservationException {
		ArrayList<Reservation> done = new ArrayList<Reservation>();
		
		try {
			for (Quote q : quotes) {
				done.add(this.confirmQuote(q));
			}
		} catch (ReservationException e) {
			for (Reservation res : done) {
				cancelReservation(res);
			}
			done.clear();
			throw new ReservationException(e.getMessage());
		}
		return done;
	}

	public void cancelReservation(Reservation res) {
		
		for (CarRentalCompany crc : getAllRentals()) {
			if (crc.getName().equals(res.getRentalCompany())) {
				crc.cancelReservation(res);
			}
		}

	}

	/**
	 * Get all reservations made by the given car renter.
	 *
	 * @param renter
	 *            name of the car renter
	 * @return the list of reservations of the given car renter
	 */
	public List<Reservation> getReservations(String renter) {
		//  use persistence instead

		/*
		 * List<Reservation> out = new ArrayList<Reservation>();
		 * 
		 * for (CarRentalCompany crc : CRCS.values()) { for (Car c :
		 * crc.getCars()) { for (Reservation r : c.getReservations()) { if
		 * (r.getCarRenter().equals(renter)) { out.add(r); } } } }
		 * 
		 * return out;
		 */

		List<Reservation> out = new ArrayList<Reservation>();

		for (CarRentalCompany crc : getAllRentals()) {
			EntityManager em = EMF.get().createEntityManager();
			List<Car> cars;
			try {
				Query query = em.createNamedQuery("Rental.FindAllCarsForCompany", Car.class);
				query.setParameter("company", crc.getKey());
				cars = query.getResultList();

			} finally {
				em.close();
			}
			for (Car c : cars) {
				for (Reservation r : c.getReservations()) {
					if (r.getCarRenter().equals(renter)) {
						out.add(r);
					}
				}
			}
		}

		return out;

	}

	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param crcName
	 *            the given car rental company
	 * @return The list of car types in the given car rental company.
	 */
	public Collection<CarType> getCarTypesOfCarRentalCompany(String crcName) {
		//  use persistence instead

		/*
		 * CarRentalCompany crc = CRCS.get(crcName); Collection<CarType> out =
		 * new ArrayList<CarType>(crc.getAllCarTypes()); return out;
		 */

		Collection<CarType> types = null;
		EntityManager em = EMF.get().createEntityManager();
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(crcName)) {
					Query query = em.createNamedQuery("Rental.FindAllCarTypesForCompany", CarType.class);
					query.setParameter("company", crc.getKey());
					types = new ArrayList<CarType>(query.getResultList());
					return types;
				}
			}

			return types;
		} finally {
			em.close();
		}
	}

	/**
	 * Get the list of cars of the given car type in the given car rental
	 * company.
	 *
	 * @param crcName
	 *            name of the car rental company
	 * @param carType
	 *            the given car type
	 * @return A list of car IDs of cars with the given car type.
	 */
	public Collection<Integer> getCarIdsByCarType(String crcName, CarType carType) {
		Collection<Integer> out = new ArrayList<Integer>();
		for (Car c : getCarsByCarType(crcName, carType)) {
			out.add(c.getId());
		}
		return out;
	}

	/**
	 * Get the amount of cars of the given car type in the given car rental
	 * company.
	 *
	 * @param crcName
	 *            name of the car rental company
	 * @param carType
	 *            the given car type
	 * @return A number, representing the amount of cars of the given car type.
	 */
	public int getAmountOfCarsByCarType(String crcName, CarType carType) {
		return this.getCarsByCarType(crcName, carType).size();
	}

	/**
	 * Get the list of cars of the given car type in the given car rental
	 * company.
	 *
	 * @param crcName
	 *            name of the car rental company
	 * @param carType
	 *            the given car type
	 * @return List of cars of the given car type
	 */
	private List<Car> getCarsByCarType(String crcName, CarType carType) {
		// use persistence instead

		/*
		 * List<Car> out = new ArrayList<Car>(); for (CarRentalCompany crc :
		 * CRCS.values()) { for (Car c : crc.getCars()) { if (c.getType() ==
		 * carType) { out.add(c); } } } return out;
		 */

		List<Car> out = new ArrayList<Car>();
		EntityManager em = EMF.get().createEntityManager();
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				Query query = em.createNamedQuery("Rental.FindAllCarsForCompany", Car.class);
				query.setParameter("company", crc.getKey());
				List<Car> cars = query.getResultList();
				for (Car c : cars) {
					if (c.getType() == carType) {
						out.add(c);
					}
				}
			}
			return out;
		} finally {
			em.close();
		}
	}

	/**
	 * Check whether the given car renter has reservations.
	 *
	 * @param renter
	 *            the car renter
	 * @return True if the number of reservations of the given car renter is
	 *         higher than 0. False otherwise.
	 */
	public boolean hasReservations(String renter) {
		return this.getReservations(renter).size() > 0;
	}
}