package pt.unl.fct.di.apdc.helpinghand.data.model;

public class CreateEvent {

    public String name;
    public String description;
    public String start;
    public String end;
    public double[] location;

    public CreateEvent() {}

    public CreateEvent(String name, String description,String start, String end,
                        double[] location) {
        this.name = name;
        this.description = description;
        this.start = start;
        this.end = end;
        this.location = location;
    }
}
