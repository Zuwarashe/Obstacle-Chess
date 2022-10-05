/**
* This class is for testing purposes and provides a CLI to play
* the game from the console
*/
public class TestCLI {
	private Chess yourChess; // Test chess object

	/**
	* This allows the class to be a self-contained testing unit
	*/
	public static void main(String[] args) {
		Chess gameToTest = new Chess();
		TestCLI myTester = new TestCLI(gameToTest);
		myTester.run();
	}

	/**
	* Testing Constructor
	* @param yc The chess object to use during the test
	*/
	public TestCLI(Chess yc) {
		yourChess = yc;
	}

	public void run() {
		/*
		*********************************
		* 			CLI MODE 			*
		* 	  FOR TESTING PURPOSES		*
		*********************************
		*/
		System.out.println("*********************************");
		System.out.println("*           CLI MODE            *");
		System.out.println("*      FOR TESTING PURPOSES     *");
		System.out.println("*********************************");
		System.out.println();
		if(yourChess == null) {
			System.out.println("TEST ERROR: null chess object");
			return;
		}
		java.util.Scanner sc = new java.util.Scanner(System.in);
		String move = "";
		yourChess.printBoard();
		while(true) {
			System.out.print("Move: ");
			move = sc.nextLine();
			if(move.equals("exit")) {
				System.exit(0);
			}
			if(move.equals("restore")) {
				yourChess.restoreBoard();
				yourChess.printBoard();
				continue;
			}
			if(move.equals("help")) {
				System.out.println("Available Commands:");
				System.out.println();
				System.out.println("help      : Shows a list of available commands");
				System.out.println("exit      : Exits the game");
				System.out.println("restore   : Restores the game to the revious state");
				System.out.println("rs [x]    : Restores the game to point X");
				System.out.println("gb [file] : Load a board");
				System.out.println("gl [file] : Load a game log and executes it");
				System.out.println("sb [file] : Save the game board");
				System.out.println("sl [file] : Save the game log");
				continue;
			}
			// Run Commands
			String[] cmd = move.split(" ");
			if(cmd.length == 2) {
				switch(cmd[0]) {
					case "rs":
						try {
							if(yourChess.restoreBoard(Integer.parseInt(cmd[1]))) {
								yourChess.printBoard();
							}
						} catch(Exception ex) {
							System.out.println("ERROR: invalid restore point");
						}
						break;
					case "gb":
						if(yourChess.loadGame(cmd[1])) {
							yourChess.printBoard();
						}
						break;
					case "gl":
						if(yourChess.runGame(cmd[1])) {
							yourChess.printBoard();
						}
						break;
					case "sb":
						if(yourChess.saveGame(cmd[1])) {
							yourChess.printBoard();
						}
						break;
					case "sl":
						if(yourChess.saveGameLog(cmd[1])) {
							yourChess.printBoard();
						}
						break;
					default:
						System.out.println("ERROR: unknown command");
						break;
				}
				continue;
			}
			yourChess.move(move);
			yourChess.printBoard();
		}
	}
}