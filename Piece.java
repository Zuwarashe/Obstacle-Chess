/**
* This is the piece class that is used to represent 
* the individual pieces
*/
public class Piece {
	private char chessPiece; // The chess piece
	private String path; // The path to the image

	/**
	* Default constructor
	* @param pc The chess piece character
	* @param loc The location of the image file
	*/
	public Piece(char pc, String loc) {
		chessPiece = pc;
		path = loc;
	}
	
	/**
	* Gets the chess piece type
	* @return The chess piece character
	*/
	public char getPiece() {
		return chessPiece;
	}

	/**
	* Get the image file path
	* @return The location of the image file
	*/
	public String getPath()
	{
		return path;
	}
}