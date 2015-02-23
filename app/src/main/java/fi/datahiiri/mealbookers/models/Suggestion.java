package fi.datahiiri.mealbookers.models;

/**
 * Created by Lisa och Ilkka on 20.2.2015.
 */
public class Suggestion {
    public int id;
    public String time;
    public User creator;
    public User[] members;
    public User[] outside_members;
    public Boolean accepted;
    public Boolean manageable;
}
