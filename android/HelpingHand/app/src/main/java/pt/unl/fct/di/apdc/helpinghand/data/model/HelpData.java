package pt.unl.fct.di.apdc.helpinghand.data.model;

public class HelpData {


    public long id;
    public String name;
    public String creator;
    public String description;
    public String time;
    public double[] location;

    public HelpData() {}


    public HelpData(long id, String name, String creator, String description, String time, double[] location) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.description = description;
        this.time = time;
        this.location = location;
    }

}
