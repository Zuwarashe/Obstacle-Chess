import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
* The chess engine that handles all the operations of the game
*/
public class Chess {
	private char activePlayer;
	private char[][] board; // Chess board
	private char[][] oBoard; // Obstacles board
	private char[][] wBoard; // Walls board
	private boolean gamePlaying; // Game play flag. True if game is playing
	private ArrayList<String> gameLog; // Keep track of all moves
	private ArrayList<Storage> boardState; // Keep track of the board state
	private boolean whiteCheck; // White king in check
	private boolean blackCheck; // Black king in check
	private boolean[][] hasMoved; // Castling detection. [0][0] - white king, [1][1] - black rook kingside
	private String enpassantPawn; // pawn vulnerable to en passant attack
	private int[] enpassantPawnGui; // pawn vulnerable to en passant attack (for gui)
	private int fiftyMoveCounter; // Counter for the fifty move rule
	private int whiteWallRemain; // Remaining white walls
	private int blackWallRemain; // Remaining black walls
	private boolean whiteTrap; // Used white trap
	private boolean blackTrap; // Used black trap
	private boolean whiteMine; // Used white mine
	private boolean blackMine; // Used black mine
	private String errMsg; // IO Error messages are stored here
	private int failedMoves; // The number of failed moves from the loaded game log
	private boolean isGui; // Set flag to enable pawn promotion dialog in gui
	private int wasTrapMine; // Return 1 [Trap] or 2 [Mine] if last move triggered an obstacle

	/**
	* The default constructor
	*/
	public Chess() {
		activePlayer = 'w';
		board = new char[8][8];
		oBoard = new char[8][8];
		wBoard = new char[8][8];
		hasMoved = new boolean[2][3];
		gamePlaying = false;
		whiteCheck = blackCheck = false;
		gameLog = new ArrayList<String>();
		boardState = new ArrayList<Storage>();
		enpassantPawn = "xx";
		enpassantPawnGui = null;
		errMsg = "";
		failedMoves = 0;
		fiftyMoveCounter = 0;
		whiteWallRemain = blackWallRemain = 3;
		whiteMine = blackMine = false;
		whiteTrap = blackTrap = false;
		isGui = false;
		wasTrapMine = 0;
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				board[i][j] = '.';
				oBoard[i][j] = '.';
				wBoard[i][j] = '.';
			}
		}
		for(int i = 0; i < hasMoved.length; i++) {
			for(int j = 0; j < hasMoved[i].length; j++) {
				hasMoved[i][j] = false;
			}
		}
		newGame();
	}

	/**
	* This constructor handles the command line interface
	* @param inBoardFile The input board file
	* @param gameFile The game file
	* @param outBoardFile The output board file
	*/
	public Chess(String inBoardFile, String gameFile, String outBoardFile) {
		// *** READ BOARD FILE ***
		if(!loadGame(inBoardFile)) {
			return;
		}
		// *** RUN CHECK IF CHECKMATE ON NEW BOARD ***
		if(isCheckMate()) {
			System.out.println("INFO: checkmate");
			gamePlaying = false;
			return;
		}
		// *** RUN GAME ***
		if(!runGame(gameFile)) {
			return;
		}
		// *** SAVE GAME TO BOARD FILE ***
		if(!saveGame(outBoardFile)) {
			return;
		}
	}

	/**
	* This validates the board file
	* @param bFile The board file
	* @return True if the file is valid
	*/
	private boolean boardFileValid(File bFile) {
		int lineCount = 0;
		BufferedReader reader = null;
		String fileContent = "";
		try {
			reader = new BufferedReader(new FileReader(bFile));
			while((fileContent = reader.readLine()) != null) {
				if(fileContent.charAt(0) != '%') {
					lineCount++;
				}
			}
			if(lineCount == 9) {
				return true;
			} else {
				return false;
			}
		} catch(IOException ex) {
			// Error reading file
			errMsg = ex.getMessage();
			return false;
		}
	}

	/**
	* This creates a new game and prepares the board to the
	* starting layout
	*/
	private void newGame() {
		// Game has not started yet
		gamePlaying = false;
		// Setup pawns
		for(int i = 0; i < board.length; i++) {
			board[1][i] = 'p'; // Black
			board[6][i] = 'P'; // White
		}
		// White pieces
		board[7][0] = board[7][7] = 'R'; // White Rooks
		board[7][1] = board[7][6] = 'N'; // Black Knights
		board[7][2] = board[7][5] = 'B'; // White Bishops
		board[7][3] = 'Q'; // White Queen
		board[7][4] = 'K'; // White King

		// Black pieces
		board[0][0] = board[0][7] = 'r'; // Black Rooks
		board[0][1] = board[0][6] = 'n'; // Black Knights
		board[0][2] = board[0][5] = 'b'; // Black Bishops
		board[0][3] = 'q'; // Black Queen
		board[0][4] = 'k'; // Black King
	}

	/**
	* Loads a game from the given board file
	* @param filename The file to load the game from
	* @return True if the game is successfully loaded
	*/
	public boolean loadGame(String filename) {
		// *** Initialise object variables ***
		board = new char[8][8];
		oBoard = new char[8][8];
		wBoard = new char[8][8];
		hasMoved = new boolean[2][3];
		gamePlaying = false;
		whiteCheck = blackCheck = false;
		gameLog = new ArrayList<String>();
		boardState = new ArrayList<Storage>();
		File fileInboard = new File(filename);
		BufferedReader reader = null;
		String fileContent = "";
		for(int i = 0; i < hasMoved.length; i++) {
			for(int j = 0; j < hasMoved[i].length; j++) {
				hasMoved[i][j] = false;
			}
		}

		// *** FILE ERROR CHECKING ***
		if(!fileInboard.exists() || fileInboard.isDirectory()) {
			System.out.println("ERROR: " + filename + " cannot be opened");
			errMsg = filename + " cannot be opened";
			return false;
		}
		if(!boardFileValid(fileInboard)) {
			System.out.println("ERROR: " + filename + " is an invalid board file");
			errMsg = filename + " is an invalid board file";
			return false;
		}

		int lineCount = 0;
		try {
			reader = new BufferedReader(new FileReader(fileInboard));
			while((fileContent = reader.readLine()) != null) {
				if(fileContent.charAt(0) != '%') {
					if(lineCount < 8) {
						// Read board
						if(fileContent.split(" ").length == 8) {
							for(int i = 0; i < board.length; i++) {
								char sqr = fileContent.split(" ")[i].charAt(0);
								if(Character.toLowerCase(sqr) == 'k' || Character.toLowerCase(sqr) == 'q' || Character.toLowerCase(sqr) == 'r' || Character.toLowerCase(sqr) == 'n' || Character.toLowerCase(sqr) == 'b' || Character.toLowerCase(sqr) == 'p') {
									board[lineCount][i] = sqr;
									oBoard[lineCount][i] = '.';
									wBoard[lineCount][i] = '.';
								} else if(sqr == 'D' || sqr == 'O' || sqr == 'M' || sqr == 'X') {
									board[lineCount][i] = '.';
									oBoard[lineCount][i] = sqr;
									wBoard[lineCount][i] = '.';
								} else if(sqr == '.') {
									board[lineCount][i] = sqr;
									oBoard[lineCount][i] = sqr;
									wBoard[lineCount][i] = sqr;
								} else {
									// WALLS
									if(fileContent.split(" ")[i].length() > 1) {
										if(sqr == '|' && fileContent.split(" ")[i].charAt(1) == '_') {
											board[lineCount][i] = '.';
											oBoard[lineCount][i] = '.';
											wBoard[lineCount][i] = 'L';
										}
									} else {
										if(sqr == '|' || sqr == '_') {
											board[lineCount][i] = '.';
											oBoard[lineCount][i] = '.';
											wBoard[lineCount][i] = sqr;
										} else {
											// Invalid character. Setting default
											board[lineCount][i] = '.';
											oBoard[lineCount][i] = '.';
											wBoard[lineCount][i] = '.';
										}
									}
								}
							}
						} else {
							System.out.println("ERROR: " + filename + " is an invalid board file");
							errMsg = filename + " is an invalid board file";
							return false;
						}
					} else {
						// Other game data
						activePlayer = fileContent.split(" ")[0].charAt(0);
						whiteWallRemain = Integer.parseInt(fileContent.split(" ")[1]);
						blackWallRemain = Integer.parseInt(fileContent.split(" ")[2]);
						// Castling
						if(fileContent.split(" ")[3].charAt(0) == '-') {
							// White king-side castling
							hasMoved[0][1] = true;
						}
						if(fileContent.split(" ")[3].charAt(1) == '-') {
							// White queen-side castling
							hasMoved[0][2] = true;
						}
						if(hasMoved[0][1] && hasMoved[0][2]) {
							hasMoved[0][0] = true;
						}
						if(fileContent.split(" ")[3].charAt(2) == '-') {
							// Black king-side castling
							hasMoved[1][1] = true;
						}
						if(fileContent.split(" ")[3].charAt(3) == '-') {
							// Black queen-side castling
							hasMoved[1][2] = true;
						}
						if(hasMoved[1][1] && hasMoved[1][2]) {
							hasMoved[0][0] = true;
						}

						if(fileContent.split(" ")[4].charAt(0) == '-') {
							enpassantPawn = "xx";
						} else {
							enpassantPawn = fileContent.split(" ")[4];
						}

						fiftyMoveCounter = Integer.parseInt(fileContent.split(" ")[5]);
					}
					lineCount++;
				}
			}
		} catch(IOException ex) {
			// Error reading file
			System.out.println("ERROR: " + filename + " could not be read");
			errMsg = ex.getMessage();
			System.out.println("Reason: " + errMsg);
			return false;
		}

		// *** CHECK IF GAME IS IN CHECK
		if(activePlayer == 'w') {
			if(isChecked('w')) {
				whiteCheck = true;
			}
		} else {
			if(isChecked('b')) {
				blackCheck = true;
			}
		}
		return true;
	}

	/**
	* Run the game using the game log in the provided file
	* @param filename The file containing the game log
	* @return True if game ran successfully
	*/
	public boolean runGame(String filename) {
		BufferedReader reader = null;
		File fileGame = new File(filename);
		String fileContent = "";
		boolean hasRead = false; // Flag to check first line
		if(!fileGame.exists() || fileGame.isDirectory()) {
			System.out.println("ERROR: " + filename + " cannot be opened");
			errMsg = filename + " cannot be opened";
			return false;
		}

		// *** RUN GAME FROM FILE ***
		failedMoves = 0;
		try {
			reader = new BufferedReader(new FileReader(fileGame));
			while((fileContent = reader.readLine()) != null) {
				if(fileContent.charAt(0) != '%') {
					if(!hasRead) {
						if(fileContent.equals("...")) {
							activePlayer = 'b';
							continue;
						}
						hasRead = true;
					}
					if(!move(fileContent)) {
						failedMoves++;
					}
				}
			}
		} catch(IOException ex) {
			// Error reading file
			System.out.println("ERROR: " + filename + " could not be read");
			errMsg = ex.getMessage();
			System.out.println("Reason: " + errMsg);
			return false;
		}
		if(enpassantPawn.equals("xx")) {
			resetEnpassPawn();
		}
		return true;
	}

	/**
	* Save the current game to the given file
	* @param filename The file to save the game to
	* @return True if the game was successfully saves
	*/
	public boolean saveGame(String filename) {
		File fileOutboard = new File(filename);
		FileWriter writer = null;
		try {
			writer = new FileWriter(fileOutboard);
			// Write date to file
			writer.write("% Game Saved: ");
			writer.write(new SimpleDateFormat("dd-MMMM-YYYY - HH:mm:ss").format(new Date()));
			writer.write("\n");
			for(int i = 0; i < board.length; i++) {
				for(int j = 0; j < board[i].length; j++) {
					// Write board to file
					if(oBoard[i][j] == '.' && wBoard[i][j] == '.') {
						writer.write(board[i][j]);
					} else {
						if(wBoard[i][j] == '.') {
							writer.write(oBoard[i][j]);
						} else {
							if(wBoard[i][j] == 'L') {
								writer.write("|_");
							} else {
								writer.write(wBoard[i][j]);
							}
						}
					}
					if(j != (board.length - 1)) {
						writer.write(" ");
					}
				}
				writer.write("\n"); // New line
			}
			// Other Game data
			writer.write(activePlayer);
			writer.write(" ");
			writer.write(String.valueOf(whiteWallRemain));
			writer.write(" ");
			writer.write(String.valueOf(blackWallRemain));
			writer.write(" ");
			// White king-side castling
			if(hasMoved[0][1]) {
				writer.write("-");
			} else {
				writer.write("+");
			}
			// White queen-side castling
			if(hasMoved[0][2]) {
				writer.write("-");
			} else {
				writer.write("+");
			}
			// Black king-side castling
			if(hasMoved[1][1]) {
				writer.write("-");
			} else {
				writer.write("+");
			}
			// Black queen-side castling
			if(hasMoved[1][2]) {
				writer.write("-");
			} else {
				writer.write("+");
			}
			writer.write(" ");
			if(enpassantPawn.equals("xx")) {
				writer.write("- ");
			} else {
				writer.write(enpassantPawn);
				writer.write(" ");
			}
			writer.write(String.valueOf(fiftyMoveCounter));
			writer.write("\n");
			writer.write("% --- End ---");
			writer.close();
		} catch(IOException ex) {
			System.out.println("ERROR: could not save game to " + filename);
			errMsg = ex.getMessage();
			System.out.println("Reason: " + errMsg);
			return false;
		}
		return true;
	}

	/**
	* Saves the game log to a file
	* @param filename The file to save the game to
	* @return True if the game was successfully saves
	*/
	public boolean saveGameLog(String filename) {
		File fileOutboard = new File(filename);
		FileWriter writer = null;
		try {
			writer = new FileWriter(fileOutboard);
			// Write date to file
			writer.write("% Game Log Saved: ");
			writer.write(new SimpleDateFormat("dd-MMMM-YYYY - HH:mm:ss").format(new Date()));
			writer.write("\n");
			for(int i = 0; i < gameLog.size(); i++) {
				writer.write(gameLog.get(i));
				writer.write("\n");
			}
			writer.write("% --- End ---");
			writer.close();
		} catch(IOException ex) {
			System.out.println("ERROR: could not save game to " + filename);
			errMsg = ex.getMessage();
			System.out.println("Reason: " + errMsg);
			return false;
		}
		return true;
	}

	/**
	* Performs the given move
	* @param muv The move in move notation. Eg. e1-e5
	* @return Move successful or not
	*/
	public boolean move(String muv) {
		// Change player on ... 
		if(muv.equals("...")) {
			if(!gamePlaying) {
				recordState(muv);
			}
			changePlayer();
			return true;
		}

		// Add mines and trap doors
		if(muv.charAt(0) == 'M') {
			if(addMineTrap(muv.substring(1), 'M')) {
				recordState(muv);
				return true;
			} else {
				return false;
			}
		}
		if(muv.charAt(0) == 'D') {
			if(addMineTrap(muv.substring(1), 'D')) {
				recordState(muv);
				return true;
			} else {
				return false;
			}
		}

		// Add Walls
		if(muv.charAt(0) == '|' || muv.charAt(0) == '_') {
			if(muv.charAt(0) == '_') {
				return addWall(muv.substring(1), '_');
			} else {
				if(muv.charAt(1) == '_') {
					return addWall(muv.substring(2), 'L');
				} else {
					return addWall(muv.substring(1), '|');
				}
			}
		}

		// Pawn promotion from game log file
		if(muv.charAt(0) == '=' && muv.length() == 2) {
			pawnPromotion(gameLog.get(gameLog.size() - 1).substring(3), muv.charAt(1));
			return true;
		}


		// Start game
		if(!gamePlaying) {
			int gCount = 0;
			for(int i = 0; i < gameLog.size(); i++) {
				if(gameLog.get(i).charAt(0) == '=' || gameLog.get(i).charAt(0) == 'M' || 
					gameLog.get(i).charAt(0) == 'D' || gameLog.get(i).charAt(0) == '|' || gameLog.get(i).charAt(0) == '_') {
					continue;
				}
				gCount++;
			}
			if(gCount <= 1) {
				gamePlaying = true;
			} else {
				System.out.println("INFO: new game has not started");
				return false;
			}
			gamePlaying = true;
		}

		// Castling move
		if(muv.equals("0-0")) {
			boolean cstrtn = kingCastling();
			if(!cstrtn) {
				System.out.println("ERROR: illegal move " + muv);
				return cstrtn;
			} else {
				return true;
			}
		} else if(muv.equals("0-0-0")) {
			boolean cstrtn = queenCastling();
			if(!cstrtn) {
				System.out.println("ERROR: illegal move " + muv);
				return cstrtn;
			} else {
				return true;
			}
		}
		// Check if moves valid
		String[] position = muv.split("-");
		if(position.length != 2) {
			System.out.println("ERROR: invalid move " + muv);
			return false;
		}
		if(!squareValid(position[0]) || !squareValid(position[1]) || position[0].equals(position[1])) {
			System.out.println("ERROR: invalid move " + muv);
			return false;
		}

		// Add Castling move - Support for GUI
		if(activePlayer == 'w') {
			if(position[0].equals("e1") && position[1].equals("h1")) {
				return kingCastling();
			} else if(position[0].equals("e1") && position[1].equals("a1")) {
				return queenCastling();
			}
		} else {
			if(position[0].equals("e8") && position[1].equals("h8")) {
				return kingCastling();
			} else if(position[0].equals("e8") && position[1].equals("a8")) {
				return queenCastling();
			}
		}

		ArrayList<String> possibleMoves = getPossibleSquares(position[0]);
		if(possibleMoves.contains(position[1])) {
			// Moves pieces
			int oldX = getSquare(position[0])[0];
			int oldY = getSquare(position[0])[1];
			int newx = getSquare(position[1])[0];
			int newY = getSquare(position[1])[1];
			if(isPlayersTurn(board[oldX][oldY])) {
				// Save current state
				recordState(muv);

				// Fifty move rule check
				if(board[newx][newY] != '.' && (board[newx][newY] == 'P' || board[newx][newY] == 'p')) {
					// No capture and pawn not moved
					fiftyMoveCounter++;
				} else {
					// Reset counter
					fiftyMoveCounter = 0;
				}

				board[newx][newY] = board[oldX][oldY];
				board[oldX][oldY] = '.';
				if(!enpassantPawn.equals("xx")) {
					// Disable en passant. Attack now or lose ability
					if(board[newx][newY] == 'P') {
						if(board[newx+1][newY] == 'p') {
							// In front of enemy pawn
							board[newx+1][newY] = '.';
							enpassantPawnGui = new int[2];
							enpassantPawnGui[0] = newx + 1;
							enpassantPawnGui[1] = newY;
						}
					} else if(board[newx][newY] == 'p') {
						if(board[newx-1][newY] == 'P') {
							// In front of enemy pawn
							board[newx-1][newY] = '.';
							enpassantPawnGui = new int[2];
							enpassantPawnGui[0] = newx - 1;
							enpassantPawnGui[1] = newY;
						}
					}
					enpassantPawn = "xx";
				}

				// Pawn promotion
				if(!isGui) {
					// Default to queen in CLI mode
					if(((newx == 0) || newx == (board.length - 1)) && (board[newx][newY] == 'P' || board[newx][newY] == 'p')) {
						pawnPromotion(getSquare(newx, newY), 'Q');
					}
				}

				// Activate obstacles
				activateObstacles(getSquare(newx, newY));

				if(blackCheck) {
					if(isChecked('b')) {
						restoreBoard();
						System.out.println("ERROR: black king is still in check");
						return false;
					} else {
						blackCheck = false;
					}
				} else if(whiteCheck) {
					if(isChecked('w')) {
						restoreBoard();
						System.out.println("ERROR: white king is still in check");
						return false;
					} else {
						whiteCheck = false;
					}
				}
				// Check if move creates check/checkmate
				if(activePlayer == 'b') {
					if(isChecked('b')) {
						restoreBoard();
						System.out.println("ERROR: cannot put yourself in check");
						return false;
					}
					if(isChecked('w')) {
						whiteCheck = true;
					}
				} else {
					if(isChecked('w')) {
						restoreBoard();
						System.out.println("ERROR: cannot put yourself in check");
						return false;
					}
					if(isChecked('b')) {
						blackCheck = true;
					}
				}
				// Adjust castling flags
				int curPlayer = activePlayer == 'w' ? 0 : 1;
				if(activePlayer == 'w') {
					if(position[0].equals("a1")) {
						hasMoved[0][2] = true; // queen side
					} else if(position[0].equals("e1")) {
						hasMoved[0][0] = true; // king
					} else if(position[0].equals("h1")) {
						hasMoved[0][1] = true; // king side
					}
				} else {
					if(position[0].equals("a8")) {
						hasMoved[1][2] = true; // queen side
					} else if(position[0].equals("e8")) {
						hasMoved[1][0] = true; // king
					} else if(position[0].equals("h8")) {
						hasMoved[1][1] = true; // king side
					}
				}

				// Activate en passant
				if(board[newx][newY] == 'P' || board[newx][newY] == 'p') {
					if(Math.abs(oldX - newx) == 2) {
						enpassantPawn = getSquare(newx, newY);
					}
				}
				checkEndGame(); // Check if the game is over
				return true;
			} else {
				System.out.print("ERROR: It is ");
				if(activePlayer == 'w') {
					System.out.print("white's ");
				} else {
					System.out.print("black's ");
				}
				System.out.println("turn to move");
				return false;
			}
		} else {
			System.out.println("ERROR: illegal move " + muv);
			return false;
		}
	}

	/**
	* Gets a list of all possible moves a piece can make <br>
	* Also used in tutorial mode to highlight possible squares
	* @param piece The piece
	* @return An array list contain all posible moves
	*/
	public ArrayList<String> getPossibleSquares(String piece) {
		ArrayList<String> rtn = new ArrayList<String>();
		int x = getSquare(piece)[0];
		int y = getSquare(piece)[1];
		rtn.clear(); // Clear just incase
		// *** WHITE PAWNS ***
		if(board[x][y] == 'P') {
			// Move forward
			if(x == 0) {
				// Prevent errors in gui move detection
				return rtn;
			}
			if(board[x-1][y] == '.') { // Move forward
				if(!wallExists(getSquare(x,y), getSquare(x-1,y))) {
					rtn.add(getSquare((x - 1), y));
				}
				if(x == 6) {
					if(board[x-2][y] == '.') { // First move -> 2 step
						if(!wallExists(getSquare(x,y), getSquare(x-1,y)) && !wallExists(getSquare(x-1,y), getSquare(x-2,y))) {
							rtn.add(getSquare((x - 2), y));
						}
					}
				}
			}
			// Eliminate other pieces
			if(y > 0 && y < 7) {
				if(pieceColour('b',x-1,y-1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x-1,y-1))) {
						rtn.add(getSquare((x - 1), y - 1));
					}
				}
				if(pieceColour('b',x-1,y+1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x-1,y+1))) {
						rtn.add(getSquare((x - 1), y + 1));
					}
				}
			} else if(y == 0) {
				if(pieceColour('b',x-1,y+1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x-1,y+1))) {
						rtn.add(getSquare((x - 1), y + 1));
					}
				}
			} else if(y == 7) {
				if(pieceColour('b',x-1,y-1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x-1,y-1))) {
						rtn.add(getSquare((x - 1), y - 1));
					}
				}
			}
			// Check if en passant possible
			if(!enpassantPawn.equals("xx")) {
				int enpX = getSquare(enpassantPawn)[0];
				int enpY = getSquare(enpassantPawn)[1];
				if(board[enpX][enpY] == 'p') {
					// if enemy
					if(enpX == x) {
						// Pass on either side
						if(enpY == (y + 1)) {
							if(!wallExistsDiag(getSquare(x,y), getSquare(enpX-1, enpY))) {
								rtn.add(getSquare((enpX - 1), enpY));
							}
						} else if(enpY == (y - 1)) {
							if(!wallExistsDiag(getSquare(x,y), getSquare(enpX-1, enpY))) {
								rtn.add(getSquare((enpX - 1), enpY));
							}
						}
					}
				}
			}
		}

		// *** BLACK PAWNS ***
		if(board[x][y] == 'p') {
			// Move forward
			if(x == 7) {
				// Prevent errors in gui move detection
				return rtn;
			}
			if(board[x+1][y] == '.') { // Move forward
				if(!wallExists(getSquare(x,y), getSquare(x+1,y))) {
					rtn.add(getSquare((x + 1), y));
				}
				if(x == 1) {
					if(board[x+2][y] == '.') { // First move -> 2 step
						if(!wallExists(getSquare(x,y), getSquare(x+1,y)) && !wallExists(getSquare(x-1,y), getSquare(x+2,y))) {
							rtn.add(getSquare((x + 2), y));
						}
					}
				}
			}
			// Eliminate other pieces
			if(y > 0 && y < 7) {
				if(pieceColour('w',x+1,y-1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x+1,y-1))) {
						rtn.add(getSquare((x + 1), y - 1));
					}
				}
				if(pieceColour('w',x+1,y+1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x+1,y+1))) {
						rtn.add(getSquare((x + 1), y + 1));
					}
				}
			} else if(y == 0) {
				if(pieceColour('w',x+1,y+1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x+1,y+1))) {
						rtn.add(getSquare((x + 1), y + 1));
					}
				}
			} else if(y == 7) {
				if(pieceColour('w',x+1,y-1)) {
					if(!wallExistsDiag(getSquare(x,y), getSquare(x+1,y-1))) {
						rtn.add(getSquare((x + 1), y - 1));
					}
				}
			}
			// Check if en passant possible
			if(!enpassantPawn.equals("xx")) {
				int enpX = getSquare(enpassantPawn)[0];
				int enpY = getSquare(enpassantPawn)[1];
				if(board[enpX][enpY] == 'P') {
					// If enemy
					if(enpX == x) {
						// Pass on either side
						if(enpY == (y + 1)) {
							if(!wallExistsDiag(getSquare(x,y), getSquare(enpX+1, enpY))) {
								rtn.add(getSquare((enpX + 1), enpY));
							}
						} else if(enpY == (y - 1)) {
							if(!wallExistsDiag(getSquare(x,y), getSquare(enpX+1, enpY))) {
								rtn.add(getSquare((enpX + 1), enpY));
							}
						}
					}
				}
			}
		}

		// *** KINGS ***
		if(board[x][y] == 'K' || board[x][y] == 'k') {
			// Get all possibilities
			if((y + 1) < board.length) {
				if(board[x][y+1] == '.' || isEnemy(board[x][y], board[x][y+1])) {
					if(!wallExists(getSquare(x,y), getSquare(x,y+1))) {
						rtn.add(getSquare(x, y+1));
					}
				}
				if((x - 1) >= 0) {
					if(board[x-1][y+1] == '.' || isEnemy(board[x][y], board[x-1][y+1])) {
						if(!wallExistsDiag(getSquare(x,y), getSquare(x-1,y+1)))
							rtn.add(getSquare(x-1, y+1));
					}
				}
				if((x + 1) < board.length) {
					if(board[x+1][y+1] == '.' || isEnemy(board[x][y], board[x+1][y+1])) {
						if(!wallExistsDiag(getSquare(x,y), getSquare(x+1,y+1)))
							rtn.add(getSquare(x+1, y+1));
					}
				}
			}
			if((y - 1) >= 0) {
				if(board[x][y-1] == '.' || isEnemy(board[x][y], board[x][y-1])) {
					if(!wallExists(getSquare(x,y), getSquare(x,y-1))) {
						rtn.add(getSquare(x, y-1));
					}
				}
				if((x - 1) >= 0) {
					if(board[x-1][y-1] == '.' || isEnemy(board[x][y], board[x-1][y-1])) {
						if(!wallExistsDiag(getSquare(x,y), getSquare(x-1,y-1))) {
							rtn.add(getSquare(x-1, y-1));
						}
					}
				}
				if((x + 1) < board.length) {
					if(board[x+1][y-1] == '.' || isEnemy(board[x][y], board[x+1][y-1])) {
						if(!wallExistsDiag(getSquare(x,y), getSquare(x+1,y-1))) {
							rtn.add(getSquare(x+1, y-1));
						}
					}
				}
			}
			if((x - 1) >= 0) {
				if(board[x-1][y] == '.' || isEnemy(board[x][y], board[x-1][y])) {
					if(!wallExists(getSquare(x,y), getSquare(x-1,y))) {
						rtn.add(getSquare(x-1, y));
					}
				}
			}
			if((x + 1) < board.length) {
				if(board[x+1][y] == '.' || isEnemy(board[x][y], board[x+1][y])) {
					if(!wallExists(getSquare(x,y), getSquare(x+1,y))) {
						rtn.add(getSquare(x+1, y));
					}
				}
			}
		}

		// *** ROOKS ***
		if(board[x][y] == 'R' || board[x][y] == 'r') {
			for(int i = x + 1; i < board.length; i++) {
				// Down
				if(board[i][y] == '.' || isEnemy(board[x][y], board[i][y])) {
					if(!wallExists(getSquare(i-1,y), getSquare(i,y))) {
						rtn.add(getSquare(i,y));
						if(isEnemy(board[x][y], board[i][y])) {
							// Can only eliminate one enemy
							break;
						}
					} else {
						break; // Wall blocking
					}
				} else {
					break; // Stop looking
				}
			}
			for(int i = x - 1; i >= 0; i--) {
				// Up
				if(board[i][y] == '.' || isEnemy(board[x][y], board[i][y])) {
					if(!wallExists(getSquare(i+1,y), getSquare(i,y))) {
						rtn.add(getSquare(i,y));
						if(isEnemy(board[x][y], board[i][y])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break; // Stop looking
				}
			}
			for(int i = y + 1; i < board.length; i++) {
				// Right
				if(board[x][i] == '.' || isEnemy(board[x][y], board[x][i])) {
					if(!wallExists(getSquare(x,i-1), getSquare(x,i))) {
						rtn.add(getSquare(x,i));
						if(isEnemy(board[x][y], board[x][i])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break; // Stop looking
				}
			}
			for(int i = y - 1; i >= 0; i--) {
				// Left
				if(board[x][i] == '.' || isEnemy(board[x][y], board[x][i])) {
					if(!wallExists(getSquare(x,i+1), getSquare(x,i))) {
						rtn.add(getSquare(x,i));
						if(isEnemy(board[x][y], board[x][i])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break; // Stop looking
				}
			}
		}

		// *** BISHOPS ***
		if(board[x][y] == 'B' || board[x][y] == 'b') {
			int offset = 1;
			int prevX = x;
			int prevY = y;
			// Down-right
			for(int i = x + 1; i < board.length; i++) {
				if((y + offset) >= board.length) {
					break;
				}
				if(board[i][y+offset] == '.' || isEnemy(board[x][y], board[i][y+offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y+offset))) {
						rtn.add(getSquare(i,y+offset));
						if(isEnemy(board[x][y], board[i][y+offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y + offset;
				offset++;
			}

			// Up-right
			offset = 1;
			prevX = x;
			prevY = y;
			for(int i = x - 1; i >= 0; i--) {
				if((y + offset) >= board.length) {
					break;
				}
				if(board[i][y+offset] == '.' || isEnemy(board[x][y], board[i][y+offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y+offset))) {
						rtn.add(getSquare(i,y+offset));
						if(isEnemy(board[x][y], board[i][y+offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y + offset;
				offset++;
			}

			// Down-left
			offset = 1;
			prevX = x;
			prevY = y;
			for(int i = x + 1; i < board.length; i++) {
				if((y - offset) < 0) {
					break;
				}
				if(board[i][y-offset] == '.' || isEnemy(board[x][y], board[i][y-offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y-offset))) {
						rtn.add(getSquare(i,y-offset));
						if(isEnemy(board[x][y], board[i][y-offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y - offset;
				offset++;
			}

			// Up-left
			offset = 1;
			prevX = x;
			prevY = y;
			for(int i = x - 1; i >= 0; i--) {
				if((y - offset) < 0) {
					break;
				}
				if(board[i][y-offset] == '.' || isEnemy(board[x][y], board[i][y-offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y-offset))) {
						rtn.add(getSquare(i,y-offset));
						if(isEnemy(board[x][y], board[i][y-offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y - offset;
				offset++;
			}
		}

		// *** KNIGHTS ***
		if(board[x][y] == 'N' || board[x][y] == 'n') {
			// Forward-left-wide |_ _
			if((y-2) >= 0 && (x-1) >= 0) {
				if(board[x-1][y-2] == '.' || isEnemy(board[x][y], board[x-1][y-2])) {
					rtn.add(getSquare(x-1,y-2));
				}
			}

			// Forward-left-narrow ||_
			if((y-1) >= 0 && (x-2) >= 0) {
				if(board[x-2][y-1] == '.' || isEnemy(board[x][y], board[x-2][y-1])) {
					rtn.add(getSquare(x-2,y-1));
				}
			}

			// Forward-right-narrow _||
			if((y+1) < board.length && (x-2) >= 0) {
				if(board[x-2][y+1] == '.' || isEnemy(board[x][y], board[x-2][y+1])) {
					rtn.add(getSquare(x-2,y+1));
				}
			}

			// Forward-right-wide _ _|
			if((y+2) < board.length && (x-1) >= 0) {
				if(board[x-1][y+2] == '.' || isEnemy(board[x][y], board[x-1][y+2])) {
					rtn.add(getSquare(x-1,y+2));
				}
			}

			// Backward-left-wide |_ _
			if((y-2) >= 0 && (x+1) < board.length) {
				if(board[x+1][y-2] == '.' || isEnemy(board[x][y], board[x+1][y-2])) {
					rtn.add(getSquare(x+1,y-2));
				}
			}

			// Backward-left-narrow ||_
			if((y-1) >= 0 && (x+2) < board.length) {
				if(board[x+2][y-1] == '.' || isEnemy(board[x][y], board[x+2][y-1])) {
					rtn.add(getSquare(x+2,y-1));
				}
			}

			// Backward-right-narrow _||
			if((y+1) < board.length && (x+2) < board.length) {
				if(board[x+2][y+1] == '.' || isEnemy(board[x][y], board[x+2][y+1])) {
					rtn.add(getSquare(x+2,y+1));
				}
			}

			// Backward-right-wide _ _|
			if((y+2) < board.length && (x+1) < board.length) {
				if(board[x+1][y+2] == '.' || isEnemy(board[x][y], board[x+1][y+2])) {
					rtn.add(getSquare(x+1,y+2));
				}
			}
		}

		// *** QUEENS ***
		if(board[x][y] == 'Q' || board[x][y] == 'q') {
			// DIAGONALS
			int offset = 1;
			int prevX = x;
			int prevY = y;
			// Down-right
			for(int i = x + 1; i < board.length; i++) {
				if((y + offset) >= board.length) {
					break;
				}
				if(board[i][y+offset] == '.' || isEnemy(board[x][y], board[i][y+offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y+offset))) {
						rtn.add(getSquare(i,y+offset));
						if(isEnemy(board[x][y], board[i][y+offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y + offset;
				offset++;
			}

			// Up-right
			offset = 1;
			prevX = x;
			prevY = y;
			for(int i = x - 1; i >= 0; i--) {
				if((y + offset) >= board.length) {
					break;
				}
				if(board[i][y+offset] == '.' || isEnemy(board[x][y], board[i][y+offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y+offset))) {
						rtn.add(getSquare(i,y+offset));
						if(isEnemy(board[x][y], board[i][y+offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y + offset;
				offset++;
			}

			// Down-left
			offset = 1;
			prevX = x;
			prevY = y;
			for(int i = x + 1; i < board.length; i++) {
				if((y - offset) < 0) {
					break;
				}
				if(board[i][y-offset] == '.' || isEnemy(board[x][y], board[i][y-offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y-offset))) {
						rtn.add(getSquare(i,y-offset));
						if(isEnemy(board[x][y], board[i][y-offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y - offset;
				offset++;
			}

			// Up-left
			offset = 1;
			prevX = x;
			prevY = y;
			for(int i = x - 1; i >= 0; i--) {
				if((y - offset) < 0) {
					break;
				}
				if(board[i][y-offset] == '.' || isEnemy(board[x][y], board[i][y-offset])) {
					if(!wallExistsDiag(getSquare(prevX,prevY), getSquare(i,y-offset))) {
						rtn.add(getSquare(i,y-offset));
						if(isEnemy(board[x][y], board[i][y-offset])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
				prevX = i;
				prevY = y - offset;
				offset++;
			}

			// VERTICAL/HORIZONTAL
			for(int i = x + 1; i < board.length; i++) {
				// Down
				if(board[i][y] == '.' || isEnemy(board[x][y], board[i][y])) {
					if(!wallExists(getSquare(i-1,y), getSquare(i,y))) {
						rtn.add(getSquare(i,y));
						if(isEnemy(board[x][y], board[i][y])) {
							// Can only eliminate one enemy
							break;
						}
					} else {
						break; // Wall blocking
					}
				} else {
					break; // Stop looking
				}
			}
			for(int i = x - 1; i >= 0; i--) {
				// Up
				if(board[i][y] == '.' || isEnemy(board[x][y], board[i][y])) {
					if(!wallExists(getSquare(i+1,y), getSquare(i,y))) {
						rtn.add(getSquare(i,y));
						if(isEnemy(board[x][y], board[i][y])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break; // Stop looking
				}
			}
			for(int i = y + 1; i < board.length; i++) {
				// Right
				if(board[x][i] == '.' || isEnemy(board[x][y], board[x][i])) {
					if(!wallExists(getSquare(x,i-1), getSquare(x,i))) {
						rtn.add(getSquare(x,i));
						if(isEnemy(board[x][y], board[x][i])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break; // Stop looking
				}
			}
			for(int i = y - 1; i >= 0; i--) {
				// Left
				if(board[x][i] == '.' || isEnemy(board[x][y], board[x][i])) {
					if(!wallExists(getSquare(x,i+1), getSquare(x,i))) {
						rtn.add(getSquare(x,i));
						if(isEnemy(board[x][y], board[x][i])) {
							break;
						}
					} else {
						break;
					}
				} else {
					break; // Stop looking
				}
			}
		}

		// Return possible moves
		return rtn;
	}

	// ********************************************************
	// ***** FUNCTION TO GET DATA FOR GRAPHICAL INTERFACE *****
	// ********************************************************

	/** This function returns the current board
	* @return board state
	*/
	public char[][] getBoard() {
		return board;
	}

	/**
	* This function returns the obstacle board that is saved
	* @return Saved board state
	*/
	public char[][] getObstacleBoard() {
		return oBoard;
	}

	/**
	* This function returns the walls board that is saved
	* @return Saved board state
	*/
	public char[][] getWallBoard() {
		return wBoard;
	}

	/**
	* This function returns the game log
	* @return Game log
	*/
	public String[] getGameLog() {
		return gameLog.toArray(new String[0]);
	}

	/**
	* This function returns the current player
	* @return Saved current player
	*/
	public char getPlayer() {
		return activePlayer;
	}

	/**
	* This function returns the black check status
	* @return Black check status
	*/
	public boolean getBlackCheck() {
		return blackCheck;
	}

	/**
	* This function returns the white check status
	* @return White check status
	*/
	public boolean getWhiteCheck() {
		return whiteCheck;
	}

	/**
	* Return the empassant pawn piece (for gui)
	* @return En passant pawn piece
	*/
	public int[] getEnPassantPawn() {
		return enpassantPawnGui;
	}

	/**
	* Reset en passant pawn piece for gui
	*/
	public void resetEnpassPawn() {
		enpassantPawnGui = null;
	}

	/**
	* This function returns the game play status
	* @return Gameplay status
	*/
	public boolean getGamePlayStatus() {
		return gamePlaying;
	}

	/**
	* This function returns the remaining white walls that can be added
	* @return White wall remaining counter
	*/
	public int getWhiteWallRemain() {
		return whiteWallRemain;
	}
	
	/**
	* This function returns the fifty move count
	* @return Fifty move counter
	*/
	public int getFiftyMoveCounter() {
		return fiftyMoveCounter;
	}

	/**
	* This function returns the remaining black walls that can be added
	* @return Black wall remaining counter
	*/
	public int getBlackWallRemain() {
		return blackWallRemain;
	}

	/**
	* This function returns a detailed message of why the last IO operation failed
	* @return Reason for last IO operation failure
	*/
	public String getIOError() {
		return errMsg;
	}

	/**
	* This function returns the number of failed moves that occured while executing a game log
	* @return Number of failed moves
	*/
	public int getFailedMoves() {
		return failedMoves;
	}

	/**
	* This function enables the GUI flag to allow the pawn promotion dialog to work correctly
	*/
	public void setGui() {
		isGui = true;
	}

	/**
	* This function returns a flag to determine if the last move was the victim of an obstacle. 
	* This reset on function return.
	* @return 0 - No, 1 - Trap door,  - Mine Explosion
	*/
	public int getTrapMineStatus() {
		int rtn = wasTrapMine;
		wasTrapMine = 0;
		return rtn;
	}

	/**
	* Get the current position of the white king to test for a check/checkmate
	* @return The square containing the white king
	*/
	public String getWhiteKing() {
		String rtn = "";
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				if(board[i][j] == 'K') {
					rtn = getSquare(i,j);
					break;
				}
			}
		}
		return rtn;
	}

	/**
	* Get the current position of the black king to test for a check/checkmate
	* @return The square containing the black king
	*/
	public String getBlackKing() {
		String rtn = "";
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				if(board[i][j] == 'k') {
					rtn = getSquare(i,j);
					break;
				}
			}
		}
		return rtn;
	}

	/**
	* This function determines if a checkmate condition is reached
	* @return true if it's a checkmate
	*/
	public boolean isCheckMate() {
		char myLeader = '-';
		char myColour = '-';
		if(blackCheck || whiteCheck) {
			myLeader = blackCheck ? 'k' : 'K';
			myColour = blackCheck ? 'b' : 'w';
			for(int i = 0; i < board.length; i++) {
				for(int j = 0; j < board[i].length; j++) {
					if(isEnemy(myLeader, board[i][j])) {
						continue;
					} else {
						// My team
						ArrayList<String> posMoves = getPossibleSquares(getSquare(i,j));
						for(int t = 0; t < posMoves.size(); t++) {
							// Try all posibilities
							recordState("checkmate-detect");
							int newx = getSquare(posMoves.get(t))[0];
							int newY = getSquare( posMoves.get(t))[1];
							board[newx][newY] = board[i][j];
							board[i][j] = '.';
							if(isChecked(myColour)) {
								restoreBoard();
							} else {
								// Way out of check located
								restoreBoard();
								return false;
							}
						}
					}
				}
			}
		} else {
			return false; // Not in check
		}
		return true; // Nothing else possible
	}

	/**
	* This determines if the game has endedd in a stalemate
	* @return True it is a stalemate
	*/
	public boolean isStaleMate() {
		// check if only king left
		int pieceCount = 0;
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				if(activePlayer == 'w') {
					if(isEnemy('k', board[i][j])) {
						pieceCount++;
					}
				} else {
					if(isEnemy('K', board[i][j])) {
						pieceCount++;
					}
				}
			}
		}
		if(pieceCount != 1) {
			return false;
		} else {
			if(activePlayer == 'w') {
				if(getPossibleSquares(getWhiteKing()).size() == 0) {
					return true;
				} else {
					return false;
				}
			} else {
				if(getPossibleSquares(getBlackKing()).size() == 0) {
					return true;
				} else {
					return false;
				}
			}
		}
	}

	/**
	* This function determines if the king is in check
	* @param king The king to check. 'w' for white and 'b' for black
	* @return True if the king is in check
	*/
	public boolean isChecked(char king) {
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				if(king == 'w') {
					if(isEnemy('K', board[i][j])) {
						if(getPossibleSquares(getSquare(i,j)).contains(getWhiteKing())) {
							return true;
						}
					} else {
						continue;
					}
				} else {
					// Then black king
					if(isEnemy('k', board[i][j])) {
						if(getPossibleSquares(getSquare(i,j)).contains(getBlackKing())) {
							return true;
						}
					} else {
						continue;
					}
				}
			}
		}
		// Not in check
		return false;
	}

	/**
	* This function determines if a threefold repetition has occured
	* @return true if threefold repetition has occured
	*/
	public boolean threefoldRepetition() {
		int occurances = -1;
		ArrayList<String> boardStatus = new ArrayList<String>();
		for(int i = 0; i < (boardState.size() - 1); i++) {
			if(gameLog.get(i).charAt(0) == '=' || gameLog.get(i).charAt(0) == 'M' || gameLog.get(i).charAt(0) == 'D' || gameLog.get(i).charAt(0) == '|' || gameLog.get(i).charAt(0) == '_') {
				continue;
			}
			boardStatus.add(Arrays.deepToString(boardState.get(i).getBoard()));
		}
		Set<String> boardRepeat = new HashSet<String>(boardStatus);
		for(String state : boardRepeat) {
			if(Collections.frequency(boardStatus, state) >= occurances) {
				occurances = Collections.frequency(boardStatus, state);
			}
		}
		if(occurances >= 3) {
			return true;
		} else {
			return false;
		}
	}

	// *****************************************************
	// ***** END OF FUNCTIONS FOR GRAPHICAL INTERFACE *****
	// *****************************************************

	/**
	* This function checks if the game is over
	*/
	private void checkEndGame() {
		// Fifty move rule check
		if(fiftyMoveCounter >= 50) {
			System.out.println("INFO: draw due to fifty moves");
			gamePlaying = false; // End game
		}
		// Threefold repetition check
		if(threefoldRepetition()) {
			System.out.println("INFO: draw due to threefold repetition");
			gamePlaying = false;
		}
		changePlayer();
		if(isCheckMate()) {
			System.out.println("INFO: checkmate");
			gamePlaying = false;
		}
		if(isStaleMate()) {
			System.out.println("INFO: stalemate");
			gamePlaying = false;
		}
	}

	/**
	* This handles the pawn promotion process
	* @param sqr The square that contains the pawn to be promoted
	* @param pc The piece to promote the pawn to
	*/
	public void pawnPromotion(String sqr, char pc) {
		int px = getSquare(sqr)[0];
		int py = getSquare(sqr)[1];
		if(Character.isLowerCase(board[px][py])) {
			// black
			board[px][py] = Character.toLowerCase(pc);
		} else {
			// white
			board[px][py] = Character.toUpperCase(pc);
		}
		recordState("=" + pc);
		if(isGui) {
			// Check if in check for GUI update
			if(activePlayer == 'b') {
				if(isChecked('b')) {
					blackCheck = true;
				}
			} else {
				if(isChecked('w')) {
					whiteCheck = true;
				}
			}
		}
	}

	/**
	* Checks if the given square contains a piece of the given colour
	* @param col The colour of the piece to check
	* @param x The X coordinate of the piece
	* @param y The Y coordinate of the piece
	* @return True if piece is black
	*/
	public boolean pieceColour(char col, int x, int y) {
		if((x < 0) || (x > 7) || ((y < 0) || (y > 7))) {
			// Invalid location
			return false;
		}

		char piece = board[x][y];
		if(Character.toLowerCase(col) == 'w') {
			// Check if white
			if(piece == 'K' || piece == 'Q' || piece == 'R' || piece == 'N' || piece == 'B' || piece == 'P') {
				return true;
			} else {
				return false;
			}
		} else if(Character.toLowerCase(col) == 'b') {
			// Check if black
			if(piece == 'k' || piece == 'q' || piece == 'r' || piece == 'n' || piece == 'b' || piece == 'p') {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	* Checks if the other piece is an enemy piece
	* @param myself The current piece
	* @param other The other piece
	* @return Returns true is the pieces are enemies
	*/
	private boolean isEnemy(char myself, char other) {
		if(myself == '.' || other == '.') {
			return false; // Empty Square
		}

		if(Character.isLowerCase(myself)) {
			if(Character.isLowerCase(other)) {
				return false;
			} else {
				return true;
			}
		} else {
			if(Character.isLowerCase(other)) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	* This function determines is the current player is allowed to continue
	* @param piece The piece that should be moved
	* @return Boolean is true is the player is allowed to continue
	*/
	private boolean isPlayersTurn(char piece) {
		if(Character.isLowerCase(piece)) {
			if(activePlayer == 'b') {
				return true;
			} else {
				return false;
			}
		} else {
			if(activePlayer == 'w') {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	* This is used to change the current active player
	*/
	private void changePlayer() {
		if(activePlayer == 'b') {
			activePlayer = 'w';
		} else {
			activePlayer = 'b';
		}
	}

	/**
	* Perform king side castling
	* @return true if castling is successful
	*/
	private boolean kingCastling() {
		int curPlayer = activePlayer == 'w' ? 0 : 1;
		String rook = activePlayer == 'w' ? "h1" : "h8";
		String destRook = activePlayer == 'w' ? "f1" : "f8";
		if(hasMoved[curPlayer][0] || hasMoved[curPlayer][1]) {
			System.out.println("ERROR: illegal king side castling");
			return false;
		} else {
			// Perform castling
			if(getPossibleSquares(rook).contains(destRook)) {
				recordState("0-0");
				int x, y;
				if(activePlayer == 'w') {
					x = getSquare(getWhiteKing())[0];
					y = getSquare(getWhiteKing())[1];
					board[x][y] = '.';
					board[x][y+2] = 'K';
					board[x][board.length - 1] = '.';
					board[x][y+1] = 'R';
				} else {
					x = getSquare(getBlackKing())[0];
					y = getSquare(getBlackKing())[1];
					board[x][y] = '.';
					board[x][y+2] = 'k';
					board[x][board.length - 1] = '.';
					board[x][y+1] = 'r';
				}
				if(isChecked(activePlayer)) {
					restoreBoard();
					System.out.println("ERROR: cannot perform king side castling");
					return false;
				} else {
					// Check if in check
					if(activePlayer == 'b') {
						if(isChecked('w')) {
							whiteCheck = true;
						}
					} else {
						if(isChecked('b')) {
							blackCheck = true;
						}
					}
					changePlayer();
					hasMoved[curPlayer][0] = true;
					hasMoved[curPlayer][1] = true;
					return true;
				}
			} else {
				return false;
			}
		}
	}

	/**
	* Perform queen side castling
	* @return true if castling is successful
	*/
	private boolean queenCastling() {
		int curPlayer = activePlayer == 'w' ? 0 : 1;
		String rook = activePlayer == 'w' ? "a1" : "a8";
		String destRook = activePlayer == 'w' ? "d1" : "d8";
		if(hasMoved[curPlayer][0] || hasMoved[curPlayer][2]) {
			System.out.println("ERROR: illegal queen side castling");
			return false;
		} else {
			// Perform castling
			if(getPossibleSquares(rook).contains(destRook)) {
				recordState("0-0-0");
				int x, y;
				if(activePlayer == 'w') {
					x = getSquare(getWhiteKing())[0];
					y = getSquare(getWhiteKing())[1];
					board[x][y] = '.';
					board[x][y-2] = 'K';
					board[x][0] = '.';
					board[x][y-1] = 'R';
				} else {
					x = getSquare(getBlackKing())[0];
					y = getSquare(getBlackKing())[1];
					board[x][y] = '.';
					board[x][y-2] = 'k';
					board[x][0] = '.';
					board[x][y-1] = 'r';
				}			
				if(isChecked(activePlayer)) {
					restoreBoard();
					System.out.println("ERROR: cannot perform queen side castling");
					return false;
				} else {
					// Check if in check
					if(activePlayer == 'b') {
						if(isChecked('w')) {
							whiteCheck = true;
						}
					} else {
						if(isChecked('b')) {
							blackCheck = true;
						}
					}
					changePlayer();
					hasMoved[curPlayer][0] = true;
					hasMoved[curPlayer][1] = true;
					return true;
				}
			} else {
				return false;
			}
		}
	}

	/**
	* Determines if king-side castling is possible
	* @return True if castling is possible
	*/
	public boolean canKingCastle() {
		int curPlayer = activePlayer == 'w' ? 0 : 1;
		if(hasMoved[curPlayer][0] || hasMoved[curPlayer][1]) {
			return false;
		} else {
			return true;
		}
	}

	/**
	* Determines if king-side castling is possible
	* @return True if castling is possible
	*/
	public boolean canQueenCastle() {
		int curPlayer = activePlayer == 'w' ? 0 : 1;
		if(hasMoved[curPlayer][0] || hasMoved[curPlayer][2]) {
			return false;
		} else {
			return true;
		}
	}

	/**
	* Determines if there is a wall between two squares
	* @param sqr1 The first square
	* @param sqr2 The second square
	* @return True if a wall exists between the two
	*/
	private boolean wallExists(String sqr1, String sqr2) {
		int x1 = getSquare(sqr1)[0];
		int y1 = getSquare(sqr1)[1];
		int x2 = getSquare(sqr2)[0];
		int y2 = getSquare(sqr2)[1];
		if(x1 == x2) {
			// Side-by-side
			if(Math.abs(y1 - y2) == 1) {
				// Together
				if(y2 > y1) {
					// y2 on right side
					if(wBoard[x2][y2] == '|' || wBoard[x2][y2] == 'L') {
						return true; // Wall exists
					} else {
						return false; // No wall
					}
				} else {
					if(wBoard[x1][y1] == '|' || wBoard[x1][y1] == 'L') {
						return true; // Wall exists
					} else {
						return false; // No wall
					}
				}
			} else {
				return false; // Not together
			}
		} else if(y1 == y2) {
			// ontop of each other
			if(Math.abs(x1 - x2) == 1) {
				// Together
				if(x2 > x1) {
					// x2 is the square below
					if(wBoard[x1][y1] == '_' || wBoard[x1][y1] == 'L') {
						return true; // Wall exists
					}	else {
						return false; // No wall
					}
				} else {
					if(wBoard[x2][y2] == '_' || wBoard[x2][y2] == 'L') {
						return true; // Wall exists
					}	else {
						return false; // No wall
					}
				}
			} else {
				return false; // Not together
			}
		} else {
			return false; // Not next to each other
		}
	}

	/**
	* Determines if the wall exists in the diagonal position
	* @param sqr1 The first square
	* @param sqr2 The second square
	* @return True if a wall exists between the two
	*/
	private boolean wallExistsDiag(String sqr1, String sqr2) {
		// Some diagonals ignored because walls only on south and west sides
		int x1 = getSquare(sqr1)[0];
		int y1 = getSquare(sqr1)[1];
		int x2 = getSquare(sqr2)[0];
		int y2 = getSquare(sqr2)[1];
		if((Math.abs(x1 - x2) == 1) && (Math.abs(y1 - y2) == 1)) {
			// Diagonal exists
			if(x1 < x2) {
				// x1 is higher on board
				if(y1 > y2) {
					// Diagonally right
					if(wBoard[x1][y1] == 'L') {
						return true; // Wall exists
					} else {
						return false; // No wall
					}
				} else {
					// Diagonally left
					return false; // No wall
				}
			} else {
				// x1 is lower on board
				if(y1 < y2) {
					// Diagonally left
					if(wBoard[x2][y2] == 'L') {
						return true; // Wall exists
					} else {
						return false; // No wall
					}
				} else {
					// Diagonally on right
					return false;
				}
			}
		} else {
			return false; // No diagonal
		}
	}

	/**
	* Add a wall at the given square
	* @param square The square to add the wall to
	* @param wall The type of wall to add
	* @return True is successful
	*/
	private boolean addWall(String square, char wall) {
		/*
		*	WALLS
		*	| west
		*	_ south
		*	L south west combo
		*/
		int xPos = getSquare(square)[0];
		int yPos = getSquare(square)[1];
		if(activePlayer == 'w') {
			if(whiteWallRemain > 0) {
				if(wBoard[xPos][yPos] != '.') {
					// Wall already exists
					if(wBoard[xPos][yPos] != wall) {
						wBoard[xPos][yPos] = 'L';
					} else {
						wBoard[xPos][yPos] = wall; // A waste because a wall is already there
					}
				} else {
					wBoard[xPos][yPos] = wall; // Add
				}
				whiteWallRemain--;
				if(wall == 'L') {
					whiteWallRemain--; // Remove second wall
				}
				System.out.print("INFO: [");
				if(activePlayer == 'w') {
					System.out.print("white");
				} else {
					System.out.print("black");
				}
				System.out.print("] you have " + whiteWallRemain);
				if(whiteWallRemain == 1) {
					System.out.println(" wall remaining");
				} else {
					System.out.println(" walls remaining");
				}
			} else {
				System.out.println("ERROR: you have no walls remaining");
				return false;
			}
		} else {
			if(blackWallRemain > 0) {
				if(wBoard[xPos][yPos] != '.') {
					// Wall already exists
					if(wBoard[xPos][yPos] != wall) {
						wBoard[xPos][yPos] = 'L';
					} else {
						wBoard[xPos][yPos] = wall; // A waste because a wall is already there
					}
				} else {
					wBoard[xPos][yPos] = wall; // Add
				}
				blackWallRemain--;
				if(wall == 'L') {
					blackWallRemain--; // Remove second wall
				}
				System.out.print("INFO: you have " + blackWallRemain);
				if(blackWallRemain == 1) {
					System.out.println(" wall remaining");
				} else {
					System.out.println(" walls remaining");
				}
			} else {
				System.out.println("ERROR: you have no walls remaining");
				return false;
			}
		}
		return true;
	}

	/**
	* Adds a mine or trap at the given square
	* @param square The square to add the wall to
	* @param mTrap The mine or trap to be added
	* @return True if successful
	*/
	private boolean addMineTrap(String square, char mTrap) {
		int xPos = getSquare(square)[0];
		int yPos = getSquare(square)[1];
		if(mTrap == 'D') {
			if(xPos < 2 || xPos > 5) {
				System.out.println("ERROR: traps can only be placed in ranks 3 - 6");
				return false;
			}
		}
		if(mTrap == 'M') {
			if(xPos < 3 || xPos > 4) {
				System.out.println("ERROR: mines can only be placed in middle 2 ranks (4 and 5)");
				return false;
			}
		}

		if(gamePlaying) {
			System.out.println("ERROR: cannot add mine or trap after game has started");
			return false;
		}
		if(mTrap == 'D') {
			// Trap doors
			if(activePlayer == 'w' && whiteTrap) {
				System.out.println("ERROR: white has no trap doors left");
				return false;
			} else if(activePlayer == 'b' && blackTrap) {
				System.out.println("ERROR: black has no trap doors left");
				return false;
			}
		} else {
			// Mine
			if(activePlayer == 'w' && whiteMine) {
				System.out.println("ERROR: white has no mines left");
				return false;
			} else if(activePlayer == 'b' && blackMine) {
				System.out.println("ERROR: black has no mines left");
				return false;
			}
		}
		if(oBoard[xPos][yPos] == '.') {
			oBoard[xPos][yPos] = mTrap; // Just add it
		} else {
			if(oBoard[xPos][yPos] == 'D' || oBoard[xPos][yPos] == 'O') {
				if(mTrap == 'M') {
					oBoard[xPos][yPos] = 'X'; // Hidden mine/trap combo
				} else {
					oBoard[xPos][yPos] = mTrap; // Wasted trap door
				}
			} else {
				// Already a mine
				if(mTrap == 'D') {
					oBoard[xPos][yPos] = 'X';
				} else {
					oBoard[xPos][yPos] = mTrap; // Wasted mine
				}
			}
		}
		if(mTrap == 'D') {
			if(activePlayer == 'w') {
				whiteTrap = true;
			} else {
				blackTrap = true;
			}
		} else {
			if(activePlayer == 'w') {
				whiteMine = true;
			} else {
				blackMine = true;
			}
		}
		changePlayer();
		return true;
	}

	/**
	* Drops pieces down trap doors and explodes mines 
	* at if a pieces lands on the square
	* @param sqr The square a piece just landed on
	*/
	private void activateObstacles(String sqr) {
		int cordX = getSquare(sqr)[0];
		int cordY = getSquare(sqr)[1];
		// TRAP DOOR
		if(oBoard[cordX][cordY] == 'D' || oBoard[cordX][cordY] == 'O') {
			board[cordX][cordY] = '.'; // Down the hole
			if(oBoard[cordX][cordY] == 'D') {
				oBoard[cordX][cordY] = 'O'; // Open trap door
			}
			wasTrapMine = 1;
		}

		// MINES
		if(oBoard[cordX][cordY] == 'M' || oBoard[cordX][cordY] == 'X') {
			// Explode, if no wall then blow it up
			if((cordY + 1) < oBoard.length) {
				if(!wallExists(sqr, getSquare(cordX,cordY+1))) {
					board[cordX][cordY+1] = '.';
				}
				if((cordX - 1) >= 0) {
					if(!wallExists(sqr, getSquare(cordX-1, cordY+1))) {
						board[cordX-1][cordY+1] = '.';
					}
				}
				if((cordX + 1) < oBoard.length) {
					if(!wallExists(sqr, getSquare(cordX+1, cordY+1))) {
						board[cordX+1][cordY+1] = '.';
					}
				}
			}
			if((cordY - 1) >= 0) {
				if(!wallExists(sqr, getSquare(cordX, cordY-1))) {
					board[cordX][cordY-1] = '.';
				}
				if(cordX - 1 >= 0) {
					if(!wallExists(sqr, getSquare(cordX-1, cordY-1))) {
						board[cordX-1][cordY-1] = '.';
					}
				}
				if(cordX + 1 < oBoard.length) {
					if(!wallExists(sqr, getSquare(cordX+1, cordY-1))) {
						board[cordX+1][cordY-1] = '.';
					}
				}
			}
			if((cordX - 1) >= 0) {
				if(!wallExists(sqr, getSquare(cordX-1, cordY))) {
					board[cordX-1][cordY] = '.';
				}
			}
			if((cordX + 1) < oBoard.length) {
				if(!wallExists(sqr, getSquare(cordX+1, cordY))) {
					board[cordX+1][cordY] = '.';
				}
			}
			// Remove Dead piece too
			board[cordX][cordY] = '.';
			// Remove used mine
			if(oBoard[cordX][cordY] == 'X') {
				oBoard[cordX][cordY] = 'O'; // Still a trap door here
			} else {
				oBoard[cordX][cordY] = '.'; // Mine gone
			}
			wasTrapMine = 2;
		}
	}

	/**
	* Determines if a square is valid
	* @param bSquare The square to be tested
	* @return The square's validity
	*/
	private boolean squareValid(String bSquare) {
		int[] square = getSquare(bSquare);
		if(square[0] == -1 || square[1] == -1) {
			return false;
		} else {
			return true;
		}
	}

	/**
	* Returns the given square in chess notation
	* @param x The X coordinate of the piece
	* @param y The Y coordinate of the piece
	* @return A string of the square the location points to
	*/
	public static String getSquare(int x, int y) {
		if((x < 0) || (x > 7) || ((y < 0) || (y > 7))) {
			// Invalid location
			return "xx";
		}

		String rtn = "";
		switch(y) {
			case 0:
				rtn = "a";
				break;
			case 1:
				rtn = "b";
				break;
			case 2:
				rtn = "c";
				break;
			case 3:
				rtn = "d";
				break;
			case 4:
				rtn = "e";
				break;
			case 5:
				rtn = "f";
				break;
			case 6:
				rtn = "g";
				break;
			case 7:
				rtn = "h";
				break;
			default:
				rtn = "xx";
				break;
		}
		if(!rtn.equals("xx")) {
			rtn += (8 - x);
		}
		return rtn;
	}

	/**
	* Returns the board position of a given square
	* @param bSquare The board square
	* @return A int array containing the location [x,y] coordinates
	*/
	public static int[] getSquare(String bSquare) {
		int[] rtn = new int[2];
		if(bSquare.length() != 2) {
			// Error
			rtn[0] = -1; // Row [Horizontal]
			rtn[1] = -1; // Column [Vertical]
			return rtn;
		} else {
			// Check order
			try {
				rtn[0] = 8 - Integer.parseInt(Character.toString(bSquare.charAt(1)));
			} catch (NumberFormatException ex) {
				// Invalid number/order
				rtn[0] = -1; // Row [Horizontal]
				rtn[1] = -1; // Column [Vertical]
				return rtn;
			}
			switch(bSquare.toLowerCase().charAt(0)) {
				case 'a':
					rtn[1] = 0;
					break;
				case 'b':
					rtn[1] = 1;
					break;
				case 'c':
					rtn[1] = 2;
					break;
				case 'd':
					rtn[1] = 3;
					break;
				case 'e':
					rtn[1] = 4;
					break;
				case 'f':
					rtn[1] = 5;
					break;
				case 'g':
					rtn[1] = 6;
					break;
				case 'h':
					rtn[1] = 7;
					break;
				default:
					rtn[1] = -1;
					break;
			}
			return rtn;
		}
	}

	/**
	* This function saves the state of the current game <br>
	* Creates a deep copy
	* @param muv The last move played
	*/
	public void recordState(String muv) {
		gameLog.add(muv);
		boardState.add(new Storage(board, oBoard, wBoard, activePlayer, 
			blackCheck, whiteCheck, hasMoved, gamePlaying, enpassantPawn, 
			fiftyMoveCounter, whiteWallRemain, blackWallRemain, whiteMine,
			blackMine, whiteTrap, blackTrap));
	}

	/**
	* This restores the game to the last saved state
	*/
	public void restoreBoard() {
		if(boardState.size() == 0) {
			System.out.println("ERROR: no restore points available");
			return;
		}
		char[][] boardCopy = boardState.get(boardState.size() - 1).getBoard();
		char[][] oBoardCopy = boardState.get(boardState.size() - 1).getOBoard();
		char[][] wBoardCopy = boardState.get(boardState.size() - 1).getWBoard();
		boolean[][] hasPlayedCopy = boardState.get(boardState.size() - 1).getHasPlayed();
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				board[i][j] = boardCopy[i][j];
				oBoard[i][j] = oBoardCopy[i][j];
				wBoard[i][j] = wBoardCopy[i][j];
			}
		}
		for(int i = 0; i < hasPlayedCopy.length; i++) {
			for(int j = 0; j < hasPlayedCopy[i].length; j++) {
				hasMoved[i][j] = hasPlayedCopy[i][j];
			}
		}
		activePlayer = boardState.get(boardState.size() - 1).getPlayer();
		blackCheck = boardState.get(boardState.size() - 1).getBlackCheck();
		whiteCheck = boardState.get(boardState.size() - 1).getWhiteCheck();
		gamePlaying = boardState.get(boardState.size() - 1).getGamePlaying();
		enpassantPawn = boardState.get(boardState.size() - 1).getEnPassPawn();
		fiftyMoveCounter = boardState.get(boardState.size() - 1).getFiftyCounter();
		whiteWallRemain = boardState.get(boardState.size() - 1).getWWRemain();
		blackWallRemain = boardState.get(boardState.size() - 1).getBWRemain();
		whiteMine = boardState.get(boardState.size() - 1).getWMine();
		blackMine = boardState.get(boardState.size() - 1).getBMine();
		whiteTrap = boardState.get(boardState.size() - 1).getWTrap();
		blackTrap = boardState.get(boardState.size() - 1).getBTrap();
		String savPoint = gameLog.get(gameLog.size() - 1);
		boardState.remove(boardState.size() - 1);
		gameLog.remove(gameLog.size() - 1);
		if(savPoint.charAt(0) == '=') {
			// Undo pawn promotion
			restoreBoard();
		}
	}

	/**
	* Restore the game to the given point
	* @param pt The point to restore the game to
	* @return True if restore successful
	*/
	public boolean restoreBoard(int pt) {
		if(pt >= boardState.size()) {
			System.out.println("ERROR: could not restore game");
			return false;
		}
		char[][] boardCopy = boardState.get(pt).getBoard();
		char[][] oBoardCopy = boardState.get(pt).getOBoard();
		char[][] wBoardCopy = boardState.get(pt).getWBoard();
		boolean[][] hasPlayedCopy = boardState.get(pt).getHasPlayed();
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				board[i][j] = boardCopy[i][j];
				oBoard[i][j] = oBoardCopy[i][j];
				wBoard[i][j] = wBoardCopy[i][j];
			}
		}
		for(int i = 0; i < hasPlayedCopy.length; i++) {
			for(int j = 0; j < hasPlayedCopy[i].length; j++) {
				hasMoved[i][j] = hasPlayedCopy[i][j];
			}
		}
		activePlayer = boardState.get(pt).getPlayer();
		blackCheck = boardState.get(pt).getBlackCheck();
		whiteCheck = boardState.get(pt).getWhiteCheck();
		gamePlaying = boardState.get(pt).getGamePlaying();
		enpassantPawn = boardState.get(pt).getEnPassPawn();
		fiftyMoveCounter = boardState.get(pt).getFiftyCounter();
		whiteWallRemain = boardState.get(pt).getWWRemain();
		blackWallRemain = boardState.get(pt).getBWRemain();
		whiteMine = boardState.get(pt).getWMine();
		blackMine = boardState.get(pt).getBMine();
		whiteTrap = boardState.get(pt).getWTrap();
		blackTrap = boardState.get(pt).getBTrap();
		// Delete Everything after point
		for(int i = pt; i < boardState.size(); i++) {
			boardState.remove(boardState.size() - 1);
			gameLog.remove(gameLog.size() - 1);
		}
		return true;
	}

	/**
	* This returns the save date to view game at point x
	* @param pt The point to restore the game to
	* @return Storage data of point x, null if not found.
	*/
	public Storage getBoardState(int pt) {
		if(pt >= boardState.size()) {
			System.out.println("ERROR: board state could not be retrieved");
			return null;
		}
		return boardState.get(pt);
	}

	/**
	* This function sets the game to the saved state that is provided
	* @param state The game state to restore
	* @return True if the restoration process is successful
	*/
	public boolean setBoardState(Storage state) {
		if(state == null) {
			return false;
		}
		char[][] boardCopy = state.getBoard();
		char[][] oBoardCopy = state.getOBoard();
		char[][] wBoardCopy = state.getWBoard();
		boolean[][] hasPlayedCopy = state.getHasPlayed();
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j++) {
				board[i][j] = boardCopy[i][j];
				oBoard[i][j] = oBoardCopy[i][j];
				wBoard[i][j] = wBoardCopy[i][j];
			}
		}
		for(int i = 0; i < hasPlayedCopy.length; i++) {
			for(int j = 0; j < hasPlayedCopy[i].length; j++) {
				hasMoved[i][j] = hasPlayedCopy[i][j];
			}
		}
		activePlayer = state.getPlayer();
		blackCheck = state.getBlackCheck();
		whiteCheck = state.getWhiteCheck();
		gamePlaying = state.getGamePlaying();
		enpassantPawn = state.getEnPassPawn();
		fiftyMoveCounter = state.getFiftyCounter();
		whiteWallRemain = state.getWWRemain();
		blackWallRemain = state.getBWRemain();
		whiteMine = state.getWMine();
		blackMine = state.getBMine();
		whiteTrap = state.getWTrap();
		blackTrap = state.getBTrap();
		return true;
	}

	/**
	* Deletes the last saved state
	*/
	public void deleteLastState() {
		if(boardState.size() == 0) {
			return; // Nothing to delete
		}
		boardState.remove(boardState.size() - 1);
		gameLog.remove(gameLog.size() - 1);
	}

	/**
	* Output the current state of the chess board
	*/
	public void printBoard() {
		System.out.print("\n   A B C D E F G H  [");
		if(activePlayer == 'w') {
			System.out.print("white");
		} else {
			System.out.print("black");
		}
		if(isCheckMate()) {
			System.out.println("-CHECKMATE]");
		} else if(whiteCheck || blackCheck) {
			System.out.println("CHECK]");
		} else {
			System.out.println("]");
		}
		for(int i = 0; i < board.length; i++) {
			System.out.print((8 - i) + "  ");
			for(int j = 0; j < board[i].length; j++) {
				System.out.print(board[i][j] + " ");
			}
			System.out.println();
		}
	}
}