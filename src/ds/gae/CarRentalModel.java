package ds.gae;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import ds.gae.entities.Car;
import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.CarType;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;
import ds.gae.entities.ReservationConstraints;

public class CarRentalModel {

	private static CarRentalModel instance;

	public static CarRentalModel get() {
		if (instance == null)
			instance = new CarRentalModel();
		return instance;
	}

	private List<CarRentalCompany> getAllRentals() {
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("Rental.FindAll", CarRentalCompany.class);
		try {
			List<CarRentalCompany> crcs = query.getResultList();
			return crcs;
		} finally {
			em.close();
		}
	}

	private Set<CarType> getAllCarTypes(CarRentalCompany crc) {

		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("Rental.FindAllCarTypesForCompany", Set.class);
		query.setParameter("company", crc.getKey());
		try {
			Set<CarType> types = new HashSet<CarType>();
			List<Set<CarType>> typeSets = query.getResultList();
			if (!typeSets.isEmpty()) {
				types = typeSets.get(0);
			}
			return types;
		} finally {
			em.close();
		}
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
		// System.out.println(crcName);
		Set<String> typeNames = new HashSet<String>();
		for (CarRentalCompany crc : getAllRentals()) {
			if (crc.getName().equals(crcName)) {
				// System.out.println("Name: " + crc.getName());
				for (CarType type : getAllCarTypes(crc)) {
					typeNames.add(type.getName());
				}
				return typeNames;
			}
		}
		return typeNames;
	}

	/**
	 * Get all registered car rental companies
	 *
	 * @return the list of car rental companies
	 */
	public Collection<String> getAllRentalCompanyNames() {
		EntityManager em = EMF.get().createEntityManager();
		try {
			Query query = em.createNamedQuery("Rental.FindAllCompanyNames", String.class);
			List<String> names = query.getResultList();
			return names;
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
		for (CarRentalCompany crc : getAllRentals()) {
			if (crc.getName().equals(company)) {
				Quote out = crc.createQuote(constraints, renterName);
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

		EntityManager em = EMF.get().createEntityManager();
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(q.getRentalCompany())) {
					Reservation res = crc.confirmQuote(q);
					em.persist(res);
					em.merge(res.getCar());
					return res;
				}
			}
		} finally {
			em.close();
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
	public void confirmQuotes(List<Quote> quotes) throws ReservationException {
		
		
		
		
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
	}

	public void cancelReservation(Reservation res) {

		EntityManager em = EMF.get().createEntityManager();
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(res.getRentalCompany())) {
					// TODO Remove RES from DB
					System.out.println("REMOVED");
					Query query = em.createNamedQuery("Reservation.FindResWithKey",Reservation.class);
					query.setParameter("resKey", res.getKey());
					List<Reservation> list = query.getResultList();
					Reservation toDelete = list.get(0);
					crc.cancelReservation(toDelete);
					Car toUpdate = toDelete.getCar();
					em.remove(toDelete);
					em.merge(toUpdate);

				}
			}
		} finally {
			em.close();
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
		List<Reservation> out = new ArrayList<Reservation>();
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("Reservation.FindAllForRenter", Reservation.class);
		query.setParameter("renter", renter);
		try {
			List<Reservation> resList = query.getResultList();
			return resList;
		} finally {
			em.close();
		}
	}

	/**
	 * Get the car types available in the given car rental company.
	 *
	 * @param crcName
	 *            the given car rental company
	 * @return The list of car types in the given car rental company.
	 */
	public Collection<CarType> getCarTypesOfCarRentalCompany(String crcName) {
		Collection<CarType> types = new ArrayList<CarType>();
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("Rental.FindAllCarTypesForCompany", Set.class);
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(crcName)) {
					types = getAllCarTypes(crc);
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
		List<Car> out = new ArrayList<Car>();
		EntityManager em = EMF.get().createEntityManager();
		Query query = em.createNamedQuery("Rental.FindAllCarTypesForCompany", Set.class);
		Query query2 = em.createNamedQuery("CarType.FindAllCarsForType", Set.class);
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(crcName)) {
					query.setParameter("company", crc.getKey());
					List<Set<CarType>> typeSets = query.getResultList();
					if (!typeSets.isEmpty()) {
						for (CarType type : typeSets.get(0)) {
							if (type.equals(carType)) {
								query2.setParameter("typeKey", type.getKey());
								List<Set<Car>> carSets = query2.getResultList();
								if (!carSets.isEmpty()) {
									out.addAll(carSets.get(0));
								}
							}
						}
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