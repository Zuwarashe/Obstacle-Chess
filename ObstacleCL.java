/**
* Main Class that acts as the driver for the game
*/
public class ObstacleCL {
	private static final String className = "ObstacleCL"; // Class name for the help text
	/**
	* The driver for the game
	*/
	public static void main(String[] args) {
		Chess myGame; // Game object
		ObstacleGUI myGameGUI; // Game graphics object
		// Check command line arguments
		if(args.length == 0) {
			// Start interactive mode
			myGameGUI = new ObstacleGUI();
		} else if(args.length == 3) {
			// Start command line interface with arguments
			myGame = new Chess(args[0], args[1], args[2]);
			System.exit(0); // Exit
		} else if(args.length == 1 && args[0].equals("cli")) {
			// CLI MODE - For Testing
			TestCLI myTest = new TestCLI(new Chess());
			myTest.run();
		} else {
			// Error: Incorrect usage
			System.out.println("Usage:");
			System.out.println();
			System.out.println("AUTO MODE     : java " + className + " [Input Board File] [Game File] [Output Board File]");
			System.out.println("CLI MODE      : java " + className + " cli");
			System.out.println("GRAPHICS MODE : java " + className);
			System.exit(0); // Terminate
		}
	}
}