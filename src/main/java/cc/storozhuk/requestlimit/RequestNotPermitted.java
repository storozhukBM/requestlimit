package cc.storozhuk.requestlimit;

/**
 * @author bstorozhuk
 */
public class RequestNotPermitted extends RuntimeException {

    public RequestNotPermitted(final String message) {
        super(message);
    }
}