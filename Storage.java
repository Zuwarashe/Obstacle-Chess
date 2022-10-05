/**
* This class is used to store the state of the board 
* to allow for stepping back and forth in the game. It
* follows the Memento design pattern.
*/
public class Storage {
	private char[][] sBoard; // Board to save
	private char[][] sOBoard; // Obstacle board to save
	private char[][] sWBoard; // Wall board to save
	private char sPlayer; // Current player
	private boolean sBlackCheck; // Black check status
	private boolean sWhiteCheck; // White check status
	private boolean[][] sHasPlayed; // Castling detection to save
	private boolean sGamePlaying; // Game play flag
	private String sepPawn; // pawn vulnerable to en passant attack
	private int sFiftyCounter; // Counter for the fifty move rule
	private int sWWRemain; // Remaining white walls
	private int sBWRemain; // Remaining black walls
	private boolean sWhiteTrap; // Used white trap
	private boolean sBlackTrap; // Used black trap
	private boolean sWhiteMine; // Used white mine
	private boolean sBlackMine; // Used black mine

	/**
	* Constructor to save data using a deep copy
	* @param boardCopy The board to save
	* @param obsBoard The obstacles board to save
	* @param wallBoard The wall board to save
	* @param curPlayer The current player of this state
	* @param blkChk The black check status
	* @param whtChk The white check status
	* @param movStat The hasMoved data for castling is saved
	* @param isPlaying Gameplay status to save
	* @param enpPawn The en passant pawn variable to save
	* @param fiftyCounter The fifty move counter to save
	* @param wwRemain The white walls remaining to save
	* @param bwRemain The black walls remaining to save
	* @param wMine The white player's mine usage to save
	* @param bMine The black player's mine usage to save
	* @param wTrap The white player's trap usage to save
	* @param bTrap The black player's trap usage to save
	*/
	public Storage(char[][] boardCopy, char[][] obsBoard, char[][] wallBoard, 
		char curPlayer, boolean blkChk, boolean whtChk, boolean[][] movStat, 
		boolean isPlaying, String enpPawn, int fiftyCounter, int wwRemain, 
		int bwRemain, boolean wMine, boolean bMine, boolean wTrap, boolean bTrap) {
		// Save all data
		sBoard = new char[boardCopy.length][boardCopy[0].length];
		sOBoard = new char[obsBoard.length][obsBoard[0].length];
		sWBoard = new char[wallBoard.length][wallBoard[0].length];
		sHasPlayed = new boolean[movStat.length][movStat[0].length];
		for(int i = 0; i < boardCopy.length; i++) {
			for(int j = 0; j < boardCopy[i].length; j++) {
				sBoard[i][j] = boardCopy[i][j];
				sOBoard[i][j] = obsBoard[i][j];
				sWBoard[i][j] = wallBoard[i][j];
			}
		}
		for(int i = 0; i < movStat.length; i++) {
			for(int j = 0; j < movStat[i].length; j++) {
				sHasPlayed[i][j] = movStat[i][j];
			}
		}
		sPlayer = curPlayer;
		sBlackCheck = blkChk;
		sWhiteCheck = whtChk;
		sGamePlaying = isPlaying;
		sepPawn = enpPawn;
		sFiftyCounter = fiftyCounter;
		sWWRemain = wwRemain;
		sBWRemain = bwRemain;
		sWhiteTrap = wTrap;
		sBlackTrap = bTrap;
		sWhiteMine = wMine;
		sBlackMine = bMine;
	}

	/**
	* This function returns the board that is saved
	* @return Saved board state
	*/
	public char[][] getBoard() {
		return sBoard;
	}

	/**
	* This function returns the obstacle board that is saved
	* @return Saved board state
	*/
	public char[][] getOBoard() {
		return sOBoard;
	}

	/**
	* This function returns the walls board that is saved
	* @return Saved board state
	*/
	public char[][] getWBoard() {
		return sWBoard;
	}

	/**
	* This function returns the current player that is saved
	* @return Saved current player
	*/
	public char getPlayer() {
		return sPlayer;
	}
	/**
	* This function returns the black check status that is saved
	* @return Black check status
	*/
	public boolean getBlackCheck() {
		return sBlackCheck;
	}
	/**
	* This function returns the white check status that is saved
	* @return White check status
	*/
	public boolean getWhiteCheck() {
		return sWhiteCheck;
	}
	
	/**
	* This function returns the castling detection that is saved
	* @return Castling detection status
	*/
	public boolean[][] getHasPlayed() {
		return sHasPlayed;
	}

	/**
	* This function returns the gameplay status that is saved
	* @return Gameplay status
	*/
	public boolean getGamePlaying() {
		return sGamePlaying;
	}

	/**
	* This function returns the en passant piece that is saved
	* @return En passant piece
	*/
	public String getEnPassPawn() {
		return sepPawn;
	}

	/**
	* This function returns the fifty move counter that is saved
	* @return Fify move counter
	*/
	public int getFiftyCounter() {
		return sFiftyCounter;
	}

	/**
	* This function returns the remaining white walls that are saved
	* @return White wall remaining counter
	*/
	public int getWWRemain() {
		return sWWRemain;
	}

	/**
	* This function returns the remaining black walls that are saved
	* @return Black wall remaining counter
	*/
	public int getBWRemain() {
		return sBWRemain;
	}

	/**
	* This function returns the white trap usage that is saved
	* @return White trap usage
	*/
	public boolean getWTrap() {
		return sWhiteTrap;
	}
	/**
	* This function returns the black trap usage that is saved
	* @return Black trap usage
	*/
	public boolean getBTrap() {
		return sBlackTrap;
	}

	/**
	* This function returns the white mine usage that is saved
	* @return White mine usage
	*/
	public boolean getWMine() {
		return sWhiteMine;
	}

	/**
	* This function returns the black mine usage that is saved
	* @return Black mine usage
	*/
	public boolean getBMine() {
		return sBlackMine;
	}
}