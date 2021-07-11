package helpinghand.util.route;

import java.util.LinkedList;
import java.util.List;

import static helpinghand.util.GeneralUtils.badString;

public class CreateRoute {
	
	public String name;
	public String description;
	public List<double[]> points;
	public String[] categories;
	private boolean error = false;
	
	public CreateRoute() {}

	public CreateRoute(String name, String description, double[][] points, String[] categories) {
		this.name = name;
		this.description = description;
		this.points = new LinkedList<>();
		for(double[] point: points) {
			if(point.length != 2) {
				this.error = true;//if a point does not have only latitude and longitude
			}
			this.points.add(point);
		}
		this.categories = categories;
	}
	
	public boolean badData() {
		if(badString(name))return true;
		if(badString(description))return true;
		if(points.isEmpty()) return true;
		if(categories == null) return true;
		return error; //return true if there was an error in a point in points
	}
	
}
