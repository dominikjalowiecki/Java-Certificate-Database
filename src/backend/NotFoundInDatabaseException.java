package backend;

public class NotFoundInDatabaseException extends Exception {
	
	public NotFoundInDatabaseException() {
	}
	
	public NotFoundInDatabaseException(String errorMessage) {
		super(errorMessage);
	}
	
}