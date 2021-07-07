package pt.unl.fct.di.apdc.helpinghand.data.model;

public class CreateEvent {

    public String name;
    public String creator;
    public String description;
    public String start;
    public String end;
    public double[] location;
    public String[] conditions;

    public CreateEvent() {}

    public CreateEvent( String name, String creator, String description,String start, String end,
                        double[] location, String[] conditions) {
        this.name = name;
        this.creator = creator;
        this.description = description;
        this.start = start;
        this.end = end;
        this.location = location;
        this.conditions = conditions;
    }
}
