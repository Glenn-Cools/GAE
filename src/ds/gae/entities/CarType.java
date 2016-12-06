package ds.gae.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import com.google.appengine.api.datastore.Key;

import ds.gae.EMF;


@Entity
@NamedQuery(name = "CarType.FindAllCarsForType", query = "SELECT type.cars FROM CarType type WHERE type.key = :typeKey")
public class CarType {
    
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Key key;
	@OneToMany(mappedBy="type",cascade=CascadeType.ALL)
	private Set<Car> cars = new HashSet<Car>();
	@ManyToOne
	private CarRentalCompany company;
	@Basic
    private String name;
	@Basic
    private int nbOfSeats;
	@Basic
    private boolean smokingAllowed;
	@Basic
    private double rentalPricePerDay;
    @Basic
	//trunk space in liters
	private float trunkSpace;
    
    /***************
	 * CONSTRUCTOR *
	 ***************/
    public CarType(){
    	
    }
    
    public CarType(String name, int nbOfSeats, float trunkSpace, double rentalPricePerDay, boolean smokingAllowed,CarRentalCompany company) {
        this.name = name;
        this.nbOfSeats = nbOfSeats;
        this.trunkSpace = trunkSpace;
        this.rentalPricePerDay = rentalPricePerDay;
        this.smokingAllowed = smokingAllowed;
        this.company = company;
    }
    
    public Key getKey(){
    	return key;
    }
    
    public void addCar(Car car){
    	cars.add(car);
    }
    
    public CarRentalCompany getCompany(){
    	return company;
    }

    public String getName() {
    	return name;
    }
    
    public int getNbOfSeats() {
        return nbOfSeats;
    }
    
    public boolean isSmokingAllowed() {
        return smokingAllowed;
    }

    public double getRentalPricePerDay() {
        return rentalPricePerDay;
    }
    
    public float getTrunkSpace() {
    	return trunkSpace;
    }
    
    /*************
     * TO STRING *
     *************/
    
    @Override
    public String toString() {
    	return String.format("Car type: %s \t[seats: %d, price: %.2f, smoking: %b, trunk: %.0fl]" , 
                getName(), getNbOfSeats(), getRentalPricePerDay(), isSmokingAllowed(), getTrunkSpace());
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarType other = (CarType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}