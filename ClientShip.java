import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ClientShip {
    private static final char X= 'X';
    private static final char SQ= '\u25A0';
    private static final char D= '\u2022';
    private static final String HIT="hit";
    private static final String MISS="miss";
    private static final String WIN="win";
    private static String moveResult;
    private static String answer;
    private static String secondMessage;
    private static BufferedReader in;
    private static PrintWriter out;
    private static final Scanner scanner = new Scanner(System.in);
    private static boolean isEnded;
    private static final BattleshipBoard board = new BattleshipBoard("board.txt");

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost",2222);
            System.out.println();
            System.out.println("Your board: ");
            System.out.println(board.getBoard());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            System.out.println("Game started.");
            fisrtMove();
            handleOpponent();
            System.out.println("Enter any key to exit");
            scanner.next();
        }catch (SocketException e) {
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fisrtMove() {
        moveResult = HIT;
    }

    private static void handleOpponent() throws IOException{
        while(!isEnded){
            if (moveResult .equals(HIT) ){
                getHit();
            }
            if (moveResult.equals(MISS)){
                makeHit();
                answer = readMessage();
                if (answer.startsWith(MISS)){
                    moveResult= HIT;
                }
                if (answer.startsWith(WIN)){
                    isEnded = true;
                    break;
                }
            }
        }
    }

    private static void makeHit() throws IOException {
        System.out.println("Opponent's board:");
        System.out.println(getOpponentBoard());
        sendHit();
        System.out.println(getSecondMessage());
        System.out.println("Opponent's board after hit:");
        System.out.println(getOpponentBoard());
    }

    private static void getHit() throws IOException {
        send(board.getHiddenBoard());
        System.out.println("Your board:");
        System.out.println(board.getBoard());
        waitHit();
        send(secondMessage);
        send(board.getHiddenBoard());
        send(moveResult);
    }

    private static void waitHit() throws IOException {
        System.out.print("Opponent's move: ");
        String[] cords = readMessage().split("\\s");
        Integer row = Integer.valueOf(cords[0]);
        Integer col = Integer.valueOf(cords[1]);
        System.out.print(row+" "+col+"\n");

        if (board.isHit(row,col)){
            moveResult = HIT;
        }else {
            moveResult = MISS;
        }

        secondMessage = board.makeMove(row,col);
        System.out.println("Your board:");
        System.out.println(board.getBoard());
        if (isEnded){
            moveResult = WIN;
        }
    }

    private static void sendHit() {
        System.out.print("Your hit: ");
        send(scanner.nextLine());
    }

    private static String getSecondMessage() throws IOException {
        return readMessage();
    }
    private static String getOpponentBoard() throws IOException {
        return readMessage();
    }

    private static void send(String mess){
        out.println(countLines(mess));
        out.println(mess);
        out.flush();
    }
    private  static String readMessage() throws IOException {
        StringBuilder sb = new StringBuilder();
        int linesToRead;
        while(true){
            String readLine = in.readLine();
            if ((readLine != null) && (!readLine.isEmpty())){
                linesToRead = Integer.parseInt(readLine);
                break;
            }
        }
        for (int i = 0; i < linesToRead; i++) {
            sb.append(in.readLine()).append("\n");
        }
        return sb.toString();
    }

    private static int countLines(String str){
        String[] lines = str.split("\r\n|\r|\n");
        return  lines.length;
    }

    public static class BattleshipBoard {
        private char[][] board;
        private StringBuilder stringBuilder;

        public BattleshipBoard(String filePath) {
            getBoardFromFIle(filePath);
            generateBoard();
        }

        public void getBoardFromFIle(String filePath) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                board = new char[12][12];
                String line;
                int row = 1;
                while ((line = reader.readLine()) != null && row < 11) {
                    line = line.trim();
                    if (line.length() == 10) {
                        for (int col = 1; col < 11; col++) {
                            board[row][col] = line.charAt(col-1)=='1'?SQ:' ';
                        }
                        row++;
                    } else {
                        System.out.println("Invalid line format: " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void generateBoard() {
            stringBuilder = new StringBuilder();
            stringBuilder.append("    1   2   3   4   5   6   7   8   9   10\n");
            stringBuilder.append("  ╔═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╦═══╗\n");

            for (int i = 1; i <= 10; i++) {
                if (i!=10){
                    stringBuilder.append(i).append(" ║");
                }
                else{
                    stringBuilder.append(i).append("║");
                }
                for (int j = 1; j <= 10; j++) {
                    stringBuilder.append(" ").append(board[i][j]).append(" ║");
                }
                stringBuilder.append("\n");
                if (i != 10) {
                    stringBuilder.append("  ╠═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╬═══╣\n");
                }
            }

            stringBuilder.append("  ╚═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╩═══╝\n");
        }

        public boolean isHit(int row, int col){
            return board[row][col] == SQ;
        }

        public String makeMove(int row, int col) {
            StringBuilder result = new StringBuilder();
            if (row >= 1 && row <= 10 && col >= 1 && col <= 10) {
                if (board[row][col] ==SQ) {
                    board[row][col] = X;
                    result.append("Hit a battleship!\n");
                    if (isShipSunk(row, col)) {
                        result.append("Ship is blow up!\n");
                    }
                    if (isGameOver()){
                        result.append("Game end.\n You win!\n");
                        System.out.println("You lose.");
                        isEnded=true;
                    }
                } else if (board[row][col] == ' ') {
                    board[row][col] = D;
                    result.append("Missed!\n");
                }else{
                    System.out.println("???\n");
                }
            } else {
                result.append("Super miss!\n");
            }
            generateBoard();
            return result.toString();
        }

        private boolean isShipSunk(int row, int col) {
            Integer direction = getShipDirection(row,col);
            if (direction==0){
                 blowUpSingleShip(row,col);
                 return true;
            }
            if (direction==1){
                return tryBlowUpHorizontal(row,col);
            }
            if (direction==-1){
                tryBlowUpVertical(row,col);
            }
            return false;
        }

        private boolean tryBlowUpVertical(int row, int col) {
            Integer top=row;
            Integer bottom=row;
            char nextCh;
            while(top>1){
                nextCh = board[top-1][col];
                if (nextCh==X){
                    top--;
                    continue;
                }
                if (nextCh==SQ){
                    return false;
                }
                break;
            }

            while(bottom < 10){
                nextCh= board[bottom+1][col];
                if (nextCh == X){
                    bottom++;
                    continue;
                }
                if (nextCh ==SQ){
                    return false;
                }
                break;
            }

            blowUpVertical(top,bottom,col);

            return true;
        }

        private void blowUpVertical(Integer top, Integer bottom, int col) {
            for (int i = top-1; i <=bottom+1 ; i++) {
                board[i][col-1]=D;
                board[i][col+1]=D;
            }
            board[top-1][col]=D;
            board[bottom+1][col]=D;
        }

        private boolean tryBlowUpHorizontal(int row, int col) {
            Integer left=col;
            Integer right=col;
            char nextCh;
            while(left>1){
                 nextCh = board[row][left-1];
                if (nextCh==X){
                    left--;
                    continue;
                }
                if (nextCh==SQ){
                    return false;
                }
                break;
            }

            while(right < 10){
                nextCh= board[row][right+1];
                if (nextCh==X){
                    right++;
                    continue;
                }
                if (nextCh==SQ){
                    return false;
                }
                break;
            }

            blowUpHorizontal(left,right,row);

            return true;
        }

        private void blowUpHorizontal(Integer left, Integer right, int row) {
            for (int i = left-1; i <=right+1 ; i++) {
                board[row-1][i]=D;
                board[row+1][i]=D;
            }
            board[row][left-1]=D;
            board[row][right+1]=D;
        }

        private void blowUpSingleShip(int row, int col) {
            board[row-1][col]=D;
            board[row-1][col+1]=D;
            board[row][col+1]=D;
            board[row+1][col+1]=D;
            board[row+1][col]=D;
            board[row+1][col-1]=D;
            board[row][col-1]=D;
            board[row-1][col-1]=D;
        }

        // 1 horizontal 0 both -1 vertical
        private Integer getShipDirection(int row, int col) {
            if (board[row][col-1]==SQ||board[row][col-1]==X||board[row][col+1]==SQ||board[row][col+1]==X){
                return 1;
            }
            if(board[row-1][col]==SQ||board[row-1][col]==X||board[row+1][col]==SQ||board[row+1][col]==X){
                return -1;
            }
            return 0;
        }

        public boolean isGameOver() {
            for (int i = 1; i <= 10; i++) {
                for (int j = 1; j <= 10; j++) {
                    if (board[i][j] == SQ) {
                        return false;
                    }
                }
            }
            return true;
        }

        public String getBoard() {
            return stringBuilder.toString();
        }

        public String getHiddenBoard() {
            return getBoard().replace(SQ,' ');
        }
    }
}