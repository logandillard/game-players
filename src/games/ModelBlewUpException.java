package games;

public class ModelBlewUpException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public ModelBlewUpException() {
		super();
	}
	public ModelBlewUpException(String msg) {
		super(msg);
	}
}
