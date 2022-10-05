import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

/**
* This class is used to display the GUI
*/
public class ObstacleGUI extends JFrame implements MouseListener {
	private Chess myGame; // Chess game object
	private BoardSquare[][] boardSquares; // The array of board squares
	private BoardSquare selectedSquare; // The current selected square
	private JPanel chessBoard; // Chess Panel for the GUI
	private static final String COLS = "ABCDEFGH"; // The Letter for the squares
	private final JPanel gui = new JPanel(new BorderLayout(3, 3)); // The GUI
	private JButton newGame; // New game button
	private JButton saveBtn; // Save button
	private JButton loadBtn; // Load button
	private JButton undoBtn; // Undo button
	private JButton movesBtn; // Moves back button to goto selected move in the game log
	private JButton homeBtn; // Home button to return to current state
	private JButton mineBtn; // Button to add mines
	private JButton trapBtn; // Button to add traps
	private JButton wallBtn; // Button to add walls
	private JToggleButton tutMode; // Tutorial mode toggle
	private JLabel currentPlayer; // The current player label
	private String myMove; // The move to be made by pieces
	private boolean tutorialMode; // Tutorial mode to show all possible moves
	private JList<String> movesList; // The game log visible to the user
	private JScrollPane moveSP; //  Game log list container
	private char wallToAdd; // The wall to add
	private Storage thePast; // The past state of the game
	private Storage thePresent; // The present state of the game before going back

	/**
	* This allows the GUI to be independant and no rely on a driver class
	*/
	public static void main(String[] args) {
		// Create instance of myself
		ObstacleGUI mySelf = new ObstacleGUI();
	}

	/**
	* The default constructor
	*/
	public ObstacleGUI() {
		System.out.println("Obstacle Chess - Interactive Mode");
		myGame = new Chess();
		myGame.setGui(); // Enable gui mode
		myMove = "";
		tutorialMode = true; // On by default
		startPlaying();
	}

	/**
	* Constructor used if chess game object has been provided
	*/
	public ObstacleGUI(Chess yourGame) {
		System.out.println("Obstacle Chess - Interactive Mode");
		myGame = yourGame;
		myMove = "";
		tutorialMode = true; // On by default
		startPlaying();
	}

	/**
	* This is used to run the game
	*/
	private void startPlaying() {
		System.setOut(new PrintStream(new ByteArrayOutputStream())); // Suppress terminal output
		setupBoard(); // Setup chess board
		Runnable rnAble = new Runnable() {
			@Override
			public void run() {
				JFrame frm = new JFrame("Obstacle Chess");
				frm.add(gui);
				frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frm.setLocationByPlatform(true);
				frm.pack(); // Frame to minimum size that fits all components
				frm.setMinimumSize(frm.getSize()); // Enforce minimum size
				frm.setVisible(true);
			}
		};
		SwingUtilities.invokeLater(rnAble);
		System.setOut(new PrintStream(System.out)); // Restore terminal output stream
	}

	/**
	* This is used to set up the GUI
	*/
	private void setupBoard() {
		// Initialise Board Squares
		boardSquares = new BoardSquare[8][8];
		chessBoard = new JPanel(new GridLayout(0,9));
		movesList = new JList<String>();
		movesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Only one selection at a time
		movesList.addMouseListener(new MovesClick());
		movesList.addListSelectionListener(new ListSelect());
		moveSP = new JScrollPane();
		moveSP.setViewportView(movesList); // Add list in container
		chessBoard.setBorder(new LineBorder(Color.BLACK));
		gui.add(new JLabel(""), BorderLayout.LINE_START);
		gui.add(chessBoard);
		wallToAdd = '-';
		thePast = null;
		thePresent = null;
		newGame = new JButton("New Game");
		newGame.setToolTipText("Start new game");
		newGame.setPreferredSize(new Dimension(100, 30));
		saveBtn = new JButton("Save");
		saveBtn.setToolTipText("Save current game");
		saveBtn.setPreferredSize(new Dimension(70, 30));
		loadBtn = new JButton("Load");
		loadBtn.setToolTipText("Load saved game");
		loadBtn.setPreferredSize(new Dimension(70, 30));
		undoBtn = new JButton("Undo");
		undoBtn.setToolTipText("Undo the last move");
		tutMode = new JToggleButton("-ON-");
		tutMode.setToolTipText("Tutorial Mode");
		tutMode.setSelected(true); // Default on
		tutMode.setPreferredSize(new Dimension(70, 30));
		movesBtn = new JButton("GOTO");
		movesBtn.setToolTipText("Goto selected point");
		movesBtn.setEnabled(false); // Disabled by default
		homeBtn = new JButton("Home");
		homeBtn.setToolTipText("Return to most recent play");
		homeBtn.setEnabled(false); // Disabled by default
		mineBtn = new JButton("Add Mines");
		mineBtn.setToolTipText("Add Mines");
		trapBtn = new JButton("Add Traps");
		trapBtn.setToolTipText("Add trap doors");
		wallBtn = new JButton("Add Wall");
		wallBtn.setToolTipText("Add walls");
		currentPlayer = new JLabel("Current Player: -----");
		for(int i = 0; i < boardSquares.length; i++) {
			for(int j = 0; j < boardSquares[i].length; j++) {
				BoardSquare curSQ = new BoardSquare(i, j, getPiece(myGame.getBoard()[i][j]), myGame.getWallBoard()[i][j], myGame.getObstacleBoard()[i][j]);
				curSQ.addMouseListener(this); // To check for mouse
				boardSquares[i][j] = curSQ;
			}
		}

		// *** TOOLBAR ***
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		gui.add(toolbar, BorderLayout.PAGE_START);
		toolbar.add(newGame);
		toolbar.add(saveBtn);
		toolbar.add(loadBtn);
		toolbar.addSeparator();
		toolbar.add(new JLabel("Tutorial Mode: "));
		toolbar.add(tutMode);
		toolbar.add(Box.createHorizontalGlue()); // Force Everything after to right align
		toolbar.add(currentPlayer);
		toolbar.addSeparator();
		
		// ***  BUTTON HANDLERS ***
		newGame.addActionListener(new NewGameBtn());
		saveBtn.addActionListener(new SaveBtn());
		loadBtn.addActionListener(new LoadBtn());
		tutMode.addActionListener(new TutMode());
		movesBtn.addActionListener(new MovesBtn());
		homeBtn.addActionListener(new HomeBtn());
		undoBtn.addActionListener(new UndoBtn());
		mineBtn.addActionListener(new MinesBtn());
		trapBtn.addActionListener(new TrapsBtn());
		wallBtn.addActionListener(new WallsBtn());
		chessBoard.add(new JLabel("")); // Number Column Reservation
		for(int i = 0; i < boardSquares.length; i++) {
			chessBoard.add(new JLabel(COLS.substring(i, i+1), SwingConstants.CENTER));
		}

		for(int i = 0; i < boardSquares.length; i++) {
			for(int j = 0; j < boardSquares[i].length; j++) {
				// Add square numbers on first column
				if(j == 0) {
					chessBoard.add(new JLabel("" + (8 - i), SwingConstants.CENTER));
				}
				chessBoard.add(boardSquares[i][j]); // Chess Board Sqaures
			}
		}

		// *** GAME LOG ***
		JPanel listPanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		JPanel tmWallPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS)); // Vertical
		buttonPanel.setLayout(new FlowLayout()); // Side-by-side
		tmWallPanel.setLayout(new FlowLayout()); // Side-by-side
		listPanel.add(new JLabel("---MOVES---       ", SwingConstants.CENTER), BorderLayout.NORTH);
		listPanel.add(Box.createVerticalStrut(20)); // Spacer
		listPanel.add(moveSP, BorderLayout.SOUTH);
		buttonPanel.add(movesBtn);
		buttonPanel.add(homeBtn);
		tmWallPanel.add(mineBtn);
		tmWallPanel.add(trapBtn);
		tmWallPanel.add(wallBtn);
		listPanel.add(buttonPanel);
		listPanel.add(tmWallPanel);
		listPanel.add(undoBtn);
		listPanel.add(Box.createVerticalStrut(20)); // Spacer
		gui.add(listPanel, BorderLayout.EAST); // Add to gui

		// Get current player
		updatePlayer();
	}

	/**
	* This function starts a new game
	*@param deleteData If true then create a new game else just refresh UI elements
	*/
	private void newGame(boolean deleteData) {
		// Delete old data
		for(int i = 0; i < boardSquares.length; i++) {
			for(int j = 0; j < boardSquares[i].length; j++) {
				if(boardSquares[i][j].getPiece() != null) {
					boardSquares[i][j].removePiece();
				}
			}
		}
		if(deleteData) {
			// Start new game
			myGame = new Chess(); // new game
			myGame.setGui(); // Enable gui mode
			checkForChecks(); // Reset checked squares if any
			// Re-enable buttons
			mineBtn.setEnabled(true);
			mineBtn.setText("Add Mines");
			trapBtn.setEnabled(true);
			trapBtn.setText("Add Traps");
			wallBtn.setEnabled(true);
			wallBtn.setText("Add Walls");
			wallToAdd = '-'; // Reset
		} // else just refresh UI elements
		updatePlayer(); // Update current player
		for(int i = 0; i < boardSquares.length; i++) {
			for(int j = 0; j < boardSquares[i].length; j++) {
				// Add new elements
				Piece newPiece = getPiece(myGame.getBoard()[i][j]);
				if(newPiece != null) {
					boardSquares[i][j].setPiece(getPiece(myGame.getBoard()[i][j]));
				}
				boardSquares[i][j].setWall(myGame.getWallBoard()[i][j]);
				boardSquares[i][j].setTrapMine(myGame.getObstacleBoard()[i][j]);
				boardSquares[i][j].placeWallAndTraps();
				// Refresh
				boardSquares[i][j].select();
				boardSquares[i][j].deSelect();
			}
		}
		updateGameLog();
		checkForChecks(); // Reset checked squares if any
	}

	/**
	* This function checks if any of the players are in check, checkmate, draw or stalemate
	*/
	private void checkForChecks() {
		// Check if in check
		int[] wk = myGame.getSquare(myGame.getWhiteKing());
		int[] bk = myGame.getSquare(myGame.getBlackKing());
		if(myGame.getWhiteCheck()) {
			boardSquares[wk[0]][wk[1]].setCheck();
		}
		if(myGame.getBlackCheck()) {
			boardSquares[bk[0]][bk[1]].setCheck();
		}

		// Update labels
		if(!myGame.getWhiteCheck() && !myGame.getBlackCheck()) {
			// Nobody in check
			for(int i = 0; i < boardSquares.length; i++) {
				for(int j = 0; j < boardSquares[i].length; j++) {
					boardSquares[i][j].removeCheck();
				}
			}
		}

		// Fifty move rule check
		if(myGame.getFiftyMoveCounter() >= 50) {
			currentPlayer.setText("DRAW");
			JOptionPane.showMessageDialog(null, "Draw due to fifty moves", "DRAW", JOptionPane.INFORMATION_MESSAGE);
		}
		// Threefold repetition check
		if(myGame.threefoldRepetition()) {
			currentPlayer.setText("DRAW");
			JOptionPane.showMessageDialog(null, "Draw due to threefold repetition", "DRAW", JOptionPane.INFORMATION_MESSAGE);
		}
		if(myGame.isCheckMate()) {
			String mtitle = "";
			if(myGame.getPlayer() == 'w') {
				currentPlayer.setText("WINNER: BLACK");
				mtitle = "Black Wins";
			} else {
				currentPlayer.setText("WINNER: WHITE");
				mtitle = "White Wins";
			}
			JOptionPane.showMessageDialog(null, "Checkmate!!!", mtitle, JOptionPane.INFORMATION_MESSAGE);
		}
		if(myGame.isStaleMate()) {
			currentPlayer.setText("STALEMATE");
			JOptionPane.showMessageDialog(null, "Stalemate", "Stalemate", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	* This will update the game log on the UI
	*/
	private void updateGameLog() {
		ArrayList<String> gLog = new ArrayList<String>();
		for(int i = 0; i < myGame.getGameLog().length; i++) {
			if(myGame.getGameLog()[i].charAt(0) != 'D' && myGame.getGameLog()[i].charAt(0) != 'M' && myGame.getGameLog()[i].charAt(0) != '=' && !myGame.getGameLog()[i].equals("...")) {
				String entryL = myGame.getGameLog()[i];
				int tmStat = myGame.getTrapMineStatus();
				if(tmStat == 1) {
					entryL += " [ trap door ]";
				}  else if(tmStat == 2) {
					entryL += " [ mine - BOOM! ]";
				}
				gLog.add(entryL);
			} else {
				// Hide log while game playing
				if(myGame.getFiftyMoveCounter() >= 50 || myGame.threefoldRepetition() || myGame.isCheckMate() || myGame.isStaleMate()) {
					// Game over so safe to add
					gLog.add(myGame.getGameLog()[i]);
				} else {
					// Hide
					gLog.add("-");
				}
			}
		}
		Collections.reverse(gLog); // Reverse list to show last move first
		movesList.setListData(gLog.toArray(new String[0]));
	}

	/**
	* This function handles the pawn promotion process
	* @param pX The pawns X coordinate
	* @param pY The pawns Y coordinate
	*/
	private void pawnPromo(int pX, int pY) {
		String[] options = new String[] {"QUEEN", "ROOK", "KNIGHT", "BISHOP"};
 		int response = JOptionPane.showOptionDialog(null, "What would you like to promote pawn to?", "Pawn Promotion", 
 			JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
 		// response == 0 for Queen, 1 for Rook, 2 for Knight and 3 for Bishop.
 		if(response == 0) {
 			myGame.pawnPromotion(myGame.getSquare(pX, pY), 'Q');
 		} else if(response == 1) {
 			myGame.pawnPromotion(myGame.getSquare(pX, pY), 'R');
 		} else if(response == 2) {
 			myGame.pawnPromotion(myGame.getSquare(pX, pY), 'N');
 		} else if(response == 3) {
 			myGame.pawnPromotion(myGame.getSquare(pX, pY), 'B');
 		} else {
 			// Dialog cancelled so use queen by default
 			myGame.pawnPromotion(myGame.getSquare(pX, pY), 'Q');
 		}
 		newGame(false); // Refresh UI elements
	}

	/**
	* This function handles the process of going back in time to 
	* older moves
	*/
	private void backInTime() {
		if(movesList.getSelectedIndex() == -1) {
			return; // Just incase selection error
		}
		if(thePast == null) {
			thePresent = myGame.getBoardState(myGame.getGameLog().length - 1);
		}
		movesBtn.setEnabled(false);
		homeBtn.setEnabled(true);
		thePast = myGame.getBoardState(myGame.getGameLog().length - movesList.getSelectedIndex());
		myGame.setBoardState(thePast);
		newGame(false);
		movesBtn.setEnabled(false);
		movesList.clearSelection();
	}

	/**
	* This function creates a piece object for the given piece
	* @param pc The chess piece to create an object for
	* @return The piece object
	*/
	private Piece getPiece(char pc) {
		if(pc == '.') {
			return null; // Empty square
		}
		String imgPath = "";
		if(Character.isLowerCase(pc)) {
			imgPath += "Black_";
		} else {
			imgPath += "White_";
		}
		if(Character.toLowerCase(pc) == 'k') {
			imgPath += "King.png";
		} else if(Character.toLowerCase(pc) == 'q') {
			imgPath += "Queen.png";
		} else if(Character.toLowerCase(pc) == 'b') {
			imgPath += "Bishop.png";
		} else if(Character.toLowerCase(pc) == 'n') {
			imgPath += "Knight.png";
		} else if(Character.toLowerCase(pc) == 'r') {
			imgPath += "Rook.png";
		} else {
			// Pawns
			imgPath += "Pawn.png";
		}
		return new Piece(pc, imgPath);
	}

	/**
	* This updates the player on the interface
	*/
	private void updatePlayer() {
		if(myGame.getPlayer() == 'w') {
			currentPlayer.setText("Current Player: WHITE");
			if(myGame.getWhiteWallRemain() < 1) {
				wallBtn.setEnabled(false);
			} else {
				wallBtn.setEnabled(true);
			}
		} else if(myGame.getPlayer() == 'b') {
			currentPlayer.setText("Current Player: BLACK");
			if(myGame.getBlackWallRemain() < 1) {
				wallBtn.setEnabled(false);
			} else {
				wallBtn.setEnabled(true);
			}
		} else {
			currentPlayer.setText("Current Player: -----");
			wallBtn.setEnabled(false);
		}
		if(myGame.getWhiteCheck() || myGame.getBlackCheck()) {
			currentPlayer.setText(currentPlayer.getText() + " [CHECK]");
		}
	}
	
	/**
	* Abstract function of the parent class. 
	* On-click function is called when the user clicks on a square <br>
	* This handle all the actions of the chess board that are initiated 
	* through clicking on the board
	*/
	@Override
	public void mouseClicked(MouseEvent arg0){
		BoardSquare clickedSquare = (BoardSquare)arg0.getSource(); // Get the clicked item
		int[] sqPos = clickedSquare.getPosition();
		// Add Mines/Traps
		if(mineBtn.getText().equals("Skip") || trapBtn.getText().equals("Skip")) {
			if(mineBtn.getText().equals("Skip")) {
				if(sqPos[0] < 3 || sqPos[0] > 4) {
					JOptionPane.showMessageDialog(null, "Invalid selection", "Error", JOptionPane.ERROR_MESSAGE);
				} else {
					String muv = "M";
					muv += myGame.getSquare(sqPos[0], sqPos[1]);
					myGame.move(muv);
					if(myGame.getPlayer() == 'b') {
						JOptionPane.showMessageDialog(null, "Select a square to place a mine", "BLACK", JOptionPane.INFORMATION_MESSAGE);
					} else {
						mineBtn.setText("No Mines");
						mineBtn.setEnabled(false);
						newGame(false);
					}
					updatePlayer();
					return;
				}
			}

			if(trapBtn.getText().equals("Skip")) {
				if(sqPos[0] < 2 || sqPos[0] > 5) {
					JOptionPane.showMessageDialog(null, "Invalid selection", "Error", JOptionPane.ERROR_MESSAGE);
				} else {
					String muv = "D";
					muv += myGame.getSquare(sqPos[0], sqPos[1]);
					myGame.move(muv);
					if(myGame.getPlayer() == 'b') {
						JOptionPane.showMessageDialog(null, "Select a square to place a trap", "BLACK", JOptionPane.INFORMATION_MESSAGE);
					} else {
						trapBtn.setText("No Traps");
						trapBtn.setEnabled(false);
						newGame(false);
					}
					updatePlayer();
					return;
				}
			}
		}
		if(mineBtn.isEnabled() || trapBtn.isEnabled()) {
			mineBtn.setEnabled(false);
			mineBtn.setText("No Mines");
			trapBtn.setEnabled(false);
			trapBtn.setText("No Traps");
		}

		// Add Wall
		if(wallToAdd != '-') {
			String muv = "";
			if(wallToAdd == 'L') {
				muv = "|_";
			} else {
				muv = Character.toString(wallToAdd);
			}
			muv += myGame.getSquare(sqPos[0], sqPos[1]);
			myGame.move(muv);
			wallToAdd = '-';
			newGame(false);
			return;
		}
		if(!myGame.pieceColour(myGame.getPlayer(), sqPos[0], sqPos[1]) && selectedSquare == null) {
			return; // Not my piece
		}
		if(myGame.getFiftyMoveCounter() >= 50 || myGame.threefoldRepetition() || myGame.isCheckMate() || myGame.isStaleMate()) {
			return; // Start new game first
		}
		if(selectedSquare == null) {
			// Check if my piece and not empty
			selectedSquare = clickedSquare;
			selectedSquare.select();
			// Get posible locations
			ArrayList<String> posMoves = myGame.getPossibleSquares(myGame.getSquare(sqPos[0], sqPos[1]));

			// **** Add castling support ***
			if(Character.toLowerCase(selectedSquare.getPiece().getPiece()) == 'k') {
				// Kings
				if(myGame.canKingCastle()) {
					int[] pcpos = selectedSquare.getPosition();
					if(boardSquares[pcpos[0]][pcpos[1]+1].getPiece() == null && boardSquares[pcpos[0]][pcpos[1]+2].getPiece() == null) {
						posMoves.add("0-0");
					}
				}
				if(myGame.canQueenCastle()) {
					int[] pcpos = selectedSquare.getPosition();
					if(boardSquares[pcpos[0]][pcpos[1]-1].getPiece() == null && boardSquares[pcpos[0]][pcpos[1]-2].getPiece() == null
						&& boardSquares[pcpos[0]][pcpos[1]-3].getPiece() == null) {
						posMoves.add("0-0-0");
					}
				}
			}

			// Clean list to show only valid move (possble in check)
			ArrayList<String> cleanMoves = new ArrayList<String>();
			cleanMoves.clear();
			for(int i = 0; i < posMoves.size(); i++) {
				String testMove = myGame.getSquare(sqPos[0], sqPos[1]) + "-";
				testMove += posMoves.get(i);

				// Castling check
				if(posMoves.get(i).equals("0-0") || posMoves.get(i).equals("0-0-0")) {
					testMove = posMoves.get(i);
				}
				if(myGame.move(testMove)) {
					// Successful test move
					int[] pcpos = selectedSquare.getPosition();
					if(posMoves.get(i).equals("0-0")) {
						// Add castling to gui
						cleanMoves.add(myGame.getSquare(pcpos[0], pcpos[1]+2));
					} else if(posMoves.get(i).equals("0-0-0")) {
						cleanMoves.add(myGame.getSquare(pcpos[0], pcpos[1]-2));
					} else {
						cleanMoves.add(posMoves.get(i));
					}
					myGame.restoreBoard(); // Revert move
				}
			}

			// **** TUTORIAL MODE ****
			if(tutorialMode) {
				for(int i = 0; i < cleanMoves.size(); i++) {
					int x = myGame.getSquare(cleanMoves.get(i))[0];
					int y = myGame.getSquare(cleanMoves.get(i))[1];
					boardSquares[x][y].setPossibleDestination();
				}
			}
			myMove = myGame.getSquare(sqPos[0], sqPos[1]) + "-";
		} else {
			// If same piece clicked the deselect
			int[] savSQ = selectedSquare.getPosition();
			if(savSQ[0] == sqPos[0] && savSQ[1] == sqPos[1]) {
				// Same square
				selectedSquare.deSelect();
				for(int i = 0; i < boardSquares.length; i++) {
					for(int j = 0; j < boardSquares[i].length; j++) {
						if(boardSquares[i][j].isPossibleDestination()) {
							boardSquares[i][j].removePossibleDestination();
						}
					}
				}
				selectedSquare = null;
				return;
			}

			// Else Move piece
			myMove += myGame.getSquare(sqPos[0], sqPos[1]);

			// Castling move
			if(Character.toLowerCase(selectedSquare.getPiece().getPiece()) == 'k') {
				// Kings
				if(myGame.canKingCastle()) {
					int[] pcpos = selectedSquare.getPosition();
					if(boardSquares[pcpos[0]][pcpos[1]+1].getPiece() == null && boardSquares[pcpos[0]][pcpos[1]+2].getPiece() == null) {
						myMove = "0-0";
					}
					if(boardSquares[pcpos[0]][pcpos[1]-1].getPiece() == null && boardSquares[pcpos[0]][pcpos[1]-2].getPiece() == null
						&& boardSquares[pcpos[0]][pcpos[1]-3].getPiece() == null) {
						myMove = "0-0-0";
					}
				}
			}
			// Attempt move
			if(!myGame.move(myMove)) {
				// Deselect and try again
				myMove = ""; // Reset
				for(int i = 0; i < boardSquares.length; i++) {
					for(int j = 0; j < boardSquares[i].length; j++) {
						if(boardSquares[i][j].isPossibleDestination()) {
							boardSquares[i][j].removePossibleDestination();
						}
					}
				}
				selectedSquare.deSelect();
				selectedSquare = null;
				return;
			}
			// Success
			for(int i = 0; i < boardSquares.length; i++) {
				for(int j = 0; j < boardSquares[i].length; j++) {
					if(boardSquares[i][j].isPossibleDestination()) {
						boardSquares[i][j].removePossibleDestination();
					}
				}
			}
			selectedSquare.deSelect();
			// Eliminate piece if necessary
			if(clickedSquare.getPiece() != null) {
				// Eliminated
				clickedSquare.removePiece();
			}
			clickedSquare.setPiece(selectedSquare.getPiece());
			selectedSquare.removePiece();

			// En passant move check
			if(myGame.getEnPassantPawn() != null) {
				int enPx = myGame.getEnPassantPawn()[0];
				int enPy = myGame.getEnPassantPawn()[1];
				boardSquares[enPx][enPy].removePiece();
				// Refresh square
				boardSquares[enPx][enPy].select();
				boardSquares[enPx][enPy].deSelect();
				myGame.resetEnpassPawn();
			}

			// Pawn promotion
			int pawnX = clickedSquare.getPosition()[0];
			int pawnY = clickedSquare.getPosition()[1];
			boolean isPawn = Character.toLowerCase(clickedSquare.getPiece().getPiece()) == 'p' ? true : false;
			if(isPawn && (pawnX == 0 || pawnX == 7)) {
				pawnPromo(pawnX, pawnY);
			}

			// Castling swap rooks
			if(myMove.equals("0-0")) {
				int[] pcpos = clickedSquare.getPosition();
				boardSquares[pcpos[0]][pcpos[1]-1].setPiece(boardSquares[pcpos[0]][pcpos[1]+1].getPiece());
				boardSquares[pcpos[0]][pcpos[1]+1].removePiece();
				// Refresh
				newGame(false);
			}
			if(myMove.equals("0-0-0")) {
				int[] pcpos = clickedSquare.getPosition();
				boardSquares[pcpos[0]][pcpos[1]+1].setPiece(boardSquares[pcpos[0]][pcpos[1]-2].getPiece());
				boardSquares[pcpos[0]][pcpos[1]-2].removePiece();
				// Refresh
				newGame(false);
			}

			myMove = ""; // Reset
			selectedSquare = null;
			checkForChecks(); // Check for check, checkmate, draws or stalemate
			newGame(false); // Refresh incase fell into a trap/mine
			updatePlayer();
		}
	}

	/**
	* Abstract functions to handle mouse events. 
	* Override required to implement the mouse listener.
	*/
	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}

	/**
	* This handles the clicking of the 'New Game' button
	*/
	private class NewGameBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			int dialogResult = JOptionPane.showConfirmDialog (null, "Are you sure you want to start a new game?", 
				"New Game", JOptionPane.YES_NO_OPTION);
			if(dialogResult == JOptionPane.YES_OPTION){
				newGame(true);
			}
		}
	}

	/**
	* This handles the clicking of the 'Save' button <br>
	* The user is asked to select if the want to save the 
	* game data or game log and they select the location 
	* to save it to.
	*/
	private class SaveBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String[] options = new String[] {"Chess Board", "Game Log", "Cancel"};
			int response = JOptionPane.showOptionDialog(null, "What would you like to save?", "Save Game Data",
			        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			// response == 0 for Chess Board Data, 1 for Game Log, 2 for Cancel.
			if(response == 0 || response == 1) {
				JFrame saveFrame = new JFrame();
				saveFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				JFileChooser saveChooser = new JFileChooser() {
					@Override
					public void approveSelection() {
						File f = getSelectedFile();
						if(f.exists() && getDialogType() == SAVE_DIALOG) {
							int result = JOptionPane.showConfirmDialog(this, "The file already exists, do you want to overwrite it?", 
								"Overwrite Data", JOptionPane.YES_NO_CANCEL_OPTION);
							switch(result) {
								case JOptionPane.YES_OPTION:
									super.approveSelection();
									return;
								case JOptionPane.NO_OPTION:
									return;
								case JOptionPane.CLOSED_OPTION:
									return;
								case JOptionPane.CANCEL_OPTION:
									//cancelSelection(); // Close entire save dialog
									return;
							}
						}
						super.approveSelection();
					}
				};
				saveChooser.setDialogTitle("Save Game Data");
				saveChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
				int userOption = saveChooser.showSaveDialog(saveFrame);
				if(userOption == JFileChooser.APPROVE_OPTION) {
					File saveFile = saveChooser.getSelectedFile();
					String fileName = saveFile.getAbsolutePath();
					if(!fileName.endsWith(".txt")) {
						// Append file extension
						fileName += ".txt";
					}
					boolean saveRslt;
					if(response == 0) {
						saveRslt = myGame.saveGame(fileName);
					} else {
						saveRslt = myGame.saveGameLog(fileName);
					}
					if(saveRslt) {
						JOptionPane.showMessageDialog(null, "Game data saved successfully\n\nFile: " + fileName, "Success", JOptionPane.INFORMATION_MESSAGE);
					} else {
						String errorMsg = "An error occured while saving the file\n\nError: " + myGame.getIOError();
						JOptionPane.showMessageDialog(null, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}

	/**
	* This handles the clicking of the 'Load' button
	*/
	private class LoadBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String[] options = new String[] {"Chess Board", "Game Log", "Cancel"};
			int response = JOptionPane.showOptionDialog(null, "What would you like to load?", "Load Game Data",
			        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			// response == 0 for Chess Board Data, 1 for Game Log, 2 for Cancel.
			if(response == 0 || response == 1) {
				JFrame loadFrame = new JFrame();
				loadFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				JFileChooser loadChooser = new JFileChooser();
				loadChooser.setDialogTitle("Load Game Data");
				loadChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
				int userOption = loadChooser.showOpenDialog(loadFrame);
				if(userOption == JFileChooser.APPROVE_OPTION) {
					File loadFile = loadChooser.getSelectedFile();
					boolean loadRslt;
					if(response == 0) {
						loadRslt = myGame.loadGame(loadFile.getAbsolutePath());
					} else {
						newGame(true); // Start new game first
						loadRslt = myGame.runGame(loadFile.getAbsolutePath());
					}
					if(loadRslt) {
						if(response == 1 && myGame.getFailedMoves() > 0) {
							String msg = myGame.getFailedMoves() + " ";
							msg += myGame.getFailedMoves() > 1 ? "errors " : "error ";
							msg += "occured while running the game log";
							JOptionPane.showMessageDialog(null, msg, "Some Errors Occured", JOptionPane.ERROR_MESSAGE);
						} else {
							JOptionPane.showMessageDialog(null, "Game data loaded successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
						}
						newGame(false); // Refresh UI Elements
						// Disable Mine/Trap buttons
						if(mineBtn.isEnabled() || trapBtn.isEnabled()) {
							mineBtn.setEnabled(false);
							mineBtn.setText("No Mines");
							trapBtn.setEnabled(false);
							trapBtn.setText("No Traps");
						}

					} else {
						String errorMsg = "An error occured while loading the file\n\nError: " + myGame.getIOError();
						JOptionPane.showMessageDialog(null, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}

	/**
	* This handles the clicking of the 'Undo' button
	*/
	private class UndoBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			ArrayList<String> gamelog = new ArrayList<String>(Arrays.asList(myGame.getGameLog()));
			int i = gamelog.size() - 1;
			if(i < 0) {
				return; // Nothing to undo
			}
			if(gamelog.get(i).charAt(0) == '=' || gamelog.get(i).charAt(0) == 'M' || 
					gamelog.get(i).charAt(0) == 'D' || gamelog.get(i).charAt(0) == '|' || gamelog.get(i).charAt(0) == '_') {
				return; // Do not remove obstacles
			}
			selectedSquare = null;
			myGame.restoreBoard();
			newGame(false);
			checkForChecks();
		}
	}

	/**
	* This handles the clicking of the 'GOTO' button to load 
	* the selected move on the chess board
	*/
	private class MovesBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			backInTime();
		}
	}

	/**
	* This handles the double clicking on one of the moves in
	* the game log
	*/
	private class MovesClick implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent arg0) {
			if(arg0.getClickCount() == 2) {
				backInTime();
			}
		}
		@Override
		public void mouseEntered(MouseEvent arg0) {}
		@Override
		public void mouseExited(MouseEvent arg0) {}
		@Override
		public void mousePressed(MouseEvent arg0) {}
		@Override
		public void mouseReleased(MouseEvent arg0) {}
	}

	/**
	* This handles the clicking of the 'Home' button to return 
	* to the most recent move
	*/
	private class HomeBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			movesBtn.setEnabled(false);
			homeBtn.setEnabled(false);
			myGame.setBoardState(thePresent);
			thePast = null;
			thePresent = null;
			newGame(false);
			movesList.clearSelection();
		}
	}

	/**
	* This handles the clicking of the 'Add Mine' button
	*/
	private class MinesBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(mineBtn.getText().equals("Skip")) {
				if(myGame.getPlayer() == 'w') {
					myGame.move("...");
					JOptionPane.showMessageDialog(null, "Select a square to place a mine", "BLACK", JOptionPane.INFORMATION_MESSAGE);
				} else {
					mineBtn.setText("No Mines");
					mineBtn.setEnabled(false);
					newGame(false);
					myGame.move("...");
					updatePlayer();
					return;
				}
			}
			JOptionPane.showMessageDialog(null, "Select a square to place a mine", "WHITE", JOptionPane.INFORMATION_MESSAGE);
			mineBtn.setText("Skip");
			for(int i = 0; i < boardSquares.length; i++) {
				if(i < 3 || i > 4) {
					for(int j = 0; j < boardSquares[i].length; j++) {
						boardSquares[i][j].blackOut();
					}
				}
			}
			

		}
	}

	/**
	* This handles the clicking of the 'Add Trap' button
	*/
	private class TrapsBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if(trapBtn.getText().equals("Skip")) {
				if(myGame.getPlayer() == 'w') {
					myGame.move("...");
					JOptionPane.showMessageDialog(null, "Select a square to place a trap", "BLACK", JOptionPane.INFORMATION_MESSAGE);
				} else {
					trapBtn.setText("No Traps");
					trapBtn.setEnabled(false);
					newGame(false);
					myGame.move("...");
					updatePlayer();
					return;
				}
			}
			JOptionPane.showMessageDialog(null, "Select a square to place a trap", "WHITE", JOptionPane.INFORMATION_MESSAGE);
			trapBtn.setText("Skip");
			for(int i = 0; i < boardSquares.length; i++) {
				if(i < 2 || i > 5) {
					for(int j = 0; j < boardSquares[i].length; j++) {
						boardSquares[i][j].blackOut();
					}
				}
			}
		}
	}

	/**
	* This handles the clicking of the 'Add Wall' button
	*/
	private class WallsBtn implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String[] options = new String[] {"West", "South", "West and South"};
			int blackWall = myGame.getBlackWallRemain();
			int whiteWall = myGame.getWhiteWallRemain();
			// Remove combo if not enough walls left
			if(myGame.getPlayer() == 'w') {
				if(whiteWall < 2) {
					options = Arrays.copyOf(options, options.length-1);
				}
			} else {
				if(blackWall < 2) {
					options = Arrays.copyOf(options, options.length-1);
				}
			}
			 
			String qest = "What type of wall do you want to add?\n";
			qest += "Walls Remaining: ";
			qest += myGame.getPlayer() == 'w' ? whiteWall : blackWall;
			int response = JOptionPane.showOptionDialog(null, qest, "Add Wall",
			        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
			// response == 0 for West, 1 for South, 2 for West and South.
			if(response == 0) {
				wallToAdd = '|';
			} else if(response == 1) {
				wallToAdd = '_';
			} else if(response == 2) {
				wallToAdd = 'L';
			} else {
				wallToAdd = '-'; // Cancel
			}
			if(wallToAdd != '-') {
				JOptionPane.showMessageDialog(null, "Select a square to place the wall", "Select Square", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	* This handles the tutorial mode toggle button
	*/
	private class TutMode implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			AbstractButton toggleBtn = (AbstractButton) arg0.getSource();
			if(toggleBtn.getModel().isSelected()) {
				tutorialMode = true;
				tutMode.setSelected(true);
				tutMode.setText("-ON-");
			} else {
				tutorialMode = false;
				tutMode.setSelected(false);
				tutMode.setText("-OFF-");
			}
		}
	}

	/**
	* This enables the 'GOTO' button when an item in the game log is selected
	*/
	private class ListSelect implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent arg0) {
			// Enable button to goto list
			movesBtn.setEnabled(true);
		}
	}
}