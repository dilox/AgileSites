package wcs.core;

/**
 * Simple Arg holder class
 * 
 * @author msciab
 * 
 */
public class Arg {

	public String name;
	public String value;
	
	public Arg(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public static Arg arg(String name, String value) {
		return new Arg(name, value);
	}

}
