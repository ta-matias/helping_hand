package helpinghand.util.route;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.Value;

import static helpinghand.resources.RouteResource.ROUTE_CREATOR_PROPERTY;
import static helpinghand.resources.RouteResource.ROUTE_NAME_PROPERTY;
import static helpinghand.resources.RouteResource.ROUTE_DESCRIPTION_PROPERTY;
import static helpinghand.resources.RouteResource.ROUTE_POINTS_PROPERTY;
import static helpinghand.resources.RouteResource.ROUTE_CATEGORIES_PROPERTY;

public class Route {
	

	public String creator;
	public String name;
	public String description;
	public List<double[]> points;
	public List<String> categories;
	
	public Route() {}

	public Route(Entity route) {
		this.creator = route.getString(ROUTE_CREATOR_PROPERTY);
		this.name = route.getString(ROUTE_NAME_PROPERTY);
		this.description = route.getString(ROUTE_DESCRIPTION_PROPERTY);
		
		List<Value<LatLng>> pointsList = route.getList(ROUTE_POINTS_PROPERTY);
		this.points = pointsList.stream().map(value->new double[] {value.get().getLatitude(),value.get().getLongitude()}).collect(Collectors.toList());
		
		List<Value<String>> categoriesList = route.getList(ROUTE_CATEGORIES_PROPERTY);
		this.categories = categoriesList.stream().map(value->value.get()).collect(Collectors.toList());
	}
	
	public Route(String creator, String name, String description, double[][] points, List<String> categories) {
		this.creator = creator;
		this.name = name;
		this.description = description;
		this.points = new LinkedList<>();
		for(double[] point: points) {
			this.points.add(point);
		}
		this.categories = categories;
	}
	
	
}
