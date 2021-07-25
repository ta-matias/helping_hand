package pt.unl.fct.di.apdc.helpinghand.data.model;

public class EventData {


    public long id;
    public String name;
    public String creator;
    public String description;
    public String start;
    public String end;
    public double[] location;

    public EventData() {}

    public EventData(long id, String name, String creator, String description, String start, String end, double[] location) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.description = description;
        this.start = start;
        this.end = end;
        this.location = location;
    }
}
