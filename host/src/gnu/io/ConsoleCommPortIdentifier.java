package gnu.io;


public class ConsoleCommPortIdentifier extends CommPortIdentifier {
	public static final String NAME = "Console Testing";
	
	public ConsoleCommPortIdentifier() {
		super(NAME, null, CommPortIdentifier.PORT_SERIAL, new RXTXCommDriver());
	}
}
