package fi.datahiiri.mealbookers.models;

/**
 * Created by Lisa och Ilkka on 19.2.2015.
 */
public class PushNotification {
    public final static int NOTIFICATION_TYPE_SUGGEST = 1;
    public final static int NOTIFICATION_TYPE_CANCEL = 2;
    public final static int NOTIFICATION_TYPE_ACCEPT = 3;
    public final static int NOTIFICATION_TYPE_LEFT_ALONE = 4;

    public int id;
    public int user_id;
    public int time;
    public int type;
    public int suggestion_id;
    public int other_user_id;
    public String token;
    public Suggestion suggestion;
    public Restaurant restaurant;
    public User other_user;
    public String suggestion_time_str;
    public String restaurant_name_str;
    public String other_user_first_name;
    public String menu;
}
