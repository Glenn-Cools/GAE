package ds.gae.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Query;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.datanucleus.annotations.Unowned;

import ds.gae.EMF;

@Entity
@NamedQueries({
@NamedQuery(name = "Car.FindAllResForCar", query = "SELECT c.reservations FROM Car c WHERE c.key = :carKey")
})
public class Car {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Key key;
	@Basic
	private int id;
	@ManyToOne
	private CarType type;
	@OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
	private Set<Reservation> reservations = new HashSet<Reservation>();

	/***************
	 * CONSTRUCTOR *
	 ***************/
	public Car() {

	}

	public Car(int uid, CarType type) {
		this.id = uid;
		this.type = type;
		this.reservations = new HashSet<Reservation>();
	}

	/******
	 * ID *
	 ******/

	public int getId() {
		return id;
	}

	public Key getKey() {
		return key;
	}

	/************
	 * CAR TYPE *
	 ************/

	public CarType getType() {
		return type;
	}

	/****************
	 * RESERVATIONS *
	 ****************/

	public Set<Reservation> getReservations() {
		Set<Reservation> out = new HashSet<Reservation>();
		EntityManager em = EMF.get().createEntityManager();
		Query query3 = em.createNamedQuery("Car.FindAllResForCar", Set.class);
		try {
			query3.setParameter("carKey", this.getKey());
			List<Set<Reservation>> resSets = query3.getResultList();
			if (!resSets.isEmpty()) {
				out = resSets.get(0);
			}
			return out;
		} finally {
			em.close();
		}
	}

	public boolean isAvailable(Date start, Date end) {
		if (!start.before(end))
			throw new IllegalArgumentException("Illegal given period");

		for (Reservation reservation : getReservations()) {
			if (reservation.getEndDate().before(start) || reservation.getStartDate().after(end))
				continue;
			return false;
		}
		return true;
	}

	public void addReservation(Reservation res) {
		Set<Reservation> newRes = getReservations();
		newRes.add(res);
		reservations =  newRes;
	}

	public void removeReservation(Reservation reservation) {
		// equals-method for Reservation is required!
		Set<Reservation> newRes = getReservations();
		newRes.remove(reservation);
		reservations = newRes;
	}
}