import java.awt.*;
import javax.swing.*;

/**
 * This class is will form the squares in the board of the GUI. 
 * There's a total of 64 (8x8) squares that make up the Chess Board
 */
public class BoardSquare extends JPanel implements Cloneable {
	private static final Color whiteSQ = Color.WHITE; // White Square
	private static final Color blackSQ = new Color(100,180,100); // Black Square
	private static final Color dangerSQ = Color.RED; // Danger Square
	private static final Color trapSQ = new Color(192,192,192); // Open trap door
	private static final Color graySQ = new Color(169,169,169); // Blackout for selection
	private JLabel sqContent;
	private Piece piece;
	private int posX;
	private int posY;
	private boolean isPossibleDestination;
	private boolean isSelected;
	private boolean isInCheck;
	private char wall;
	private char trapMine;
	
	/**
	* Constructor to setup the square
	* @param x The X coordinate of the square
	* @param y The y coordinate of the square
	* @param pc The chess piece that will occupy the square
	* @param wol The wall surrounding the square
	* @param tm The trap or mine in the square
	*/
	public BoardSquare(int x, int y, Piece pc, char wol, char tm) {
		posX = x;
		posY = y;
		isInCheck = false;
		isSelected = false;
		setLayout(new BorderLayout());
		if((posX + posY) % 2 == 0) {
			setBackground(whiteSQ);
		} else {
			setBackground(blackSQ);
		}
		if(pc != null) {
			setPiece(pc);
		}
		wall = wol;
		trapMine = tm;
		placeWallAndTraps();
	}
	
	/**
	* This places a piece inside the square
	* @param pc The chess piece object
	*/
	public void setPiece(Piece pc) {
		if(pc == null) {
			// Incase of an unexpected error. Eg. corrupt board file loaded
			return;
		}
		piece = pc;
		ImageIcon img = new ImageIcon(this.getClass().getResource(pc.getPath()));
		sqContent = new JLabel(img);
		add(sqContent);
	}

	/**
	* This places a wall around the square
	* @param wol The type of wall to add
	*/
	public void setWall(char wol) {
		wall = wol;
		placeWallAndTraps();
	}

	/**
	* This places a trap or mine in the square
	* @param tm The trap or mine to add
	*/
	public void setTrapMine(char tm) {
		trapMine = tm;
		placeWallAndTraps();
	}

	/**
	* This removes the wall around the square
	*/
	public void removeWall() {
		wall = '.';
		placeWallAndTraps();
	}

	/**
	* This removes the trap or mine in the square
	*/
	public void removeTrapMine() {
		trapMine = '.';
		placeWallAndTraps();
	}
	
	/**
	* This removes the piece from the square
	*/
	public void removePiece() {
		piece = null;
		remove(sqContent);
	}
	
	/**
	* Get the piece occupying the square
	* @return The chess piece object
	*/
	public Piece getPiece() {
		return piece;
	}

	/**
	* This returns the coordinates of the square
	* @return X and Y coordinates of the square
	*/
	public int[] getPosition() {
		int[] rtn = new int[2];
		rtn[0] = posX;
		rtn[1] = posY;
		return rtn;
	}
	
	/**
	* This function highlights the piece when it is selected
	*/
	public void select() {
		setBorder(BorderFactory.createLineBorder(Color.red, 6));
		isSelected = true;
	}

	/**
	* This function determines if a square is selected
	* @return True if the square is selected
	*/
	public boolean isSelected() {
		return isSelected;
	}
	
	/**
	* This deselects the square
	*/
	public void deSelect() {
		setBorder(null);
		placeWallAndTraps();
		isSelected = false;
	}
	
	/**
	* Tutorial Mode Extension <br>
	* This highlight the square if a piece can get there
	*/
	public void setPossibleDestination() {
		setBorder(BorderFactory.createLineBorder(Color.blue, 4));
		isPossibleDestination = true;
	}
	
	/**
	* This removes the highlighting for possible destinations 
	* a piece can make
	*/
	public void removePossibleDestination() {
		setBorder(null);
		placeWallAndTraps();
		isPossibleDestination = false;
	}
	
	/**
	* This function checks if this square is a possible destination
	* @return True if this is a possible destination
	*/
	public boolean isPossibleDestination() {
		return isPossibleDestination;
	}
	
	/**
	* This function set the square in the check state to highligh it
	* For the king
	*/
	public void setCheck() {
		setBackground(dangerSQ);
		isInCheck = true;
	}
	
	/**
	* This function remove the square from the check state
	*/
	public void removeCheck() {
		setBorder(null);
		if((posX + posY) % 2 == 0) {
			setBackground(whiteSQ);
		} else {
			setBackground(blackSQ);
		}
		placeWallAndTraps();
		isInCheck = false;
	}

	/**
	* This places the walls and traps around the square
	*/
	public void placeWallAndTraps() {
		// Place walls
		int westSide = 0;
		int southSide = 0;
		if(wall == '_') {
			southSide = 3;
		} else if(wall == '|') {
			westSide = 3;
		} else if(wall == 'L') {
			westSide = 3;
			southSide = 3;
		} else {
			westSide = 0;
			southSide = 0;
		}
		setBorder(BorderFactory.createMatteBorder(0, westSide, southSide, 0, Color.BLACK));
		// Traps
		if(trapMine == 'O') {
			// Only open trap door
			setBackground(trapSQ);
		}
	}

	/**
	* This blackouts the square if it is not part of the selection process
	*/
	public void blackOut() {
		setBackground(graySQ);
	}
}