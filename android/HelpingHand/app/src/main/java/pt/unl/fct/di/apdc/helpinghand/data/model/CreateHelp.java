package pt.unl.fct.di.apdc.helpinghand.data.model;

public class CreateHelp {

        public String name;
        public String description;
        public String time;
        public double[] location;

        public CreateHelp() {}

        public CreateHelp(String name, String description, String time, double[] location) {
            this.name = name;
            this.description = description;
            this.time = time;
            this.location = location;
        }

}
