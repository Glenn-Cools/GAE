package ds.gae;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.google.appengine.api.taskqueue.DeferredTask;

import ds.gae.entities.CarRentalCompany;
import ds.gae.entities.Quote;
import ds.gae.entities.Reservation;

public class ConfirmQuotesOperation implements DeferredTask {
	
	private List<Quote> quotes;
	private static final long serialVersionUID = 1L;
	
	public ConfirmQuotesOperation(List<Quote> quotes){
		this.quotes = quotes;
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
	
	public synchronized Reservation confirmQuote(Quote q) throws ReservationException{
		EntityManager em = EMF.get().createEntityManager();
		EntityTransaction tx = em.getTransaction();
		try {
			tx.begin();
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(q.getRentalCompany())) {
					Reservation res = crc.confirmQuote(q);
					em.persist(res);
					tx.commit();
					return res;
				}
			}
		} finally {
			if(tx.isActive()){
				tx.rollback();
			}
			em.close();
			
		}

		throw new ReservationException("CarRentalCompany not found.");
	}
	
	public void cancelReservation(Reservation res) {

		EntityManager em = EMF.get().createEntityManager();
		try {
			for (CarRentalCompany crc : getAllRentals()) {
				if (crc.getName().equals(res.getRentalCompany())) {
					// TODO Remove RES from DB
					System.out.println("REMOVED");
					crc.cancelReservation(res);
					em.remove(res);
				}
			}
		} finally {
			em.close();
		}

	}

	@Override
	public void run() {
		
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
		}
		
	}

}
