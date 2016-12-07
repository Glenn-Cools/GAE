package ds.gae.entities;

import javax.persistence.*;

import com.google.appengine.api.datastore.Key;

@Entity	
public class Reservation extends Quote {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Key key;
	@Basic
	private int resId;
	@ManyToOne
	private Car car;
    
    /***************
	 * CONSTRUCTOR *
	 ***************/
	
	public Reservation(){
		
	}

    public Reservation(Quote quote,Car car) {
    	super(quote.getCarRenter(), quote.getStartDate(), quote.getEndDate(), 
    			quote.getRentalCompany(), quote.getCarType(), quote.getRentalPrice());
        this.car = car;
    }
    
    /******
     * CAR *
     ******/
    
    public Car getCar() {
    	return car;
    }
    
    public int getCarId(){
    	return car.getId();
    }
    
    /*************
     * TO STRING *
     *************/
    
    @Override
    public String toString() {
        return String.format("Reservation for %s from %s to %s at %s\nCar type: %s\tCar: %s\nTotal price: %.2f", 
                getCarRenter(), getStartDate(), getEndDate(), getRentalCompany(), getCarType(), car.getId(), getRentalPrice());
    }
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + car.getId();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;
		Reservation other = (Reservation) obj;
		if (car.getId() != other.getCar().getId())
			return false;
		return true;
	}
}