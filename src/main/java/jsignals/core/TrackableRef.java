package jsignals.core;

/**
 * A reference that can be tracked and triggered.
 * This interface is used to define a trackable reference that can be monitored for changes
 * and can also trigger updates or notifications.
 */
public interface TrackableRef extends BaseRef {

    void track();

    void trigger();

}
