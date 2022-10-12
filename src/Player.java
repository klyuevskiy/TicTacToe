import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Player {
    private Connection connection;
    private int playerNumber = 0;

    private static int startXOIndex = 3;
    final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Player(Connection connection) {
        this.connection = connection;
    }

    // старт игры, проверка что можно играть и порядок игрока
    public void start() throws SQLException {
        ResultSet xoTable = checkTable();

        // 3 лишний
        if (!isCorrectPlayersNumber(xoTable)){
            System.out.println("В игру играют уже 2 игрока");
            return;
        }

        setPlayerNumber();
        play();
    }

    // основной цикл игры
    private void play() throws SQLException{
        ResultSet xoTable;

        // пока таблица есть, то продолжаем играть
        while ((xoTable = getXOTable()) != null) {
            // столбец 2 != номеру нашего значит походил прошлый
            if (xoTable.getInt(2) != playerNumber) {
                int[] xoTableArr = xoTableToArray(xoTable);
                if (!checkWinner(xoTableArr))
                    makeMove(xoTableArr);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException){

            }
        }
    }

    private void makeMove(int[] xoTableArr) throws SQLException {
        ArrayList<Integer> moveOptions = getMoveOptions(xoTableArr);
        // нет ходов => ничья
        if (moveOptions.isEmpty()){
            System.out.println("Больше нет ходов, ничья");
            dropXOTable();
            return;
        }

        // выбираем кандидата и записываем ход
        int moveIndex = random.nextInt(0, moveOptions.size());
        int move = moveOptions.get(moveIndex);
        writeMoveToXOTableAndShowIt(move);
    }

    private int[] xoTableToArray(ResultSet xoTable) throws SQLException {
        int[] arr = new int[9];
        for (int i = startXOIndex; i < startXOIndex + 9; i++){
            arr[i - startXOIndex] = xoTable.getInt(i);
        }
        return arr;
    }

    private void writeMoveToXOTableAndShowIt(int move) throws SQLException{
        Statement statement = connection.createStatement();
        statement.execute("update xoTable set \"" + move + "\"= " + playerNumber);
        System.out.printf("%d игрок сделал ход\n", playerNumber);
        showXOTable(getXOTable());
        System.out.println();
        statement.execute("update xoTable set playerNumber = " + playerNumber);
    }

    private boolean checkWinner(int[] xoTableArr) throws SQLException {
        int winner = determineWinner(xoTableArr);
        if (winner != 0){
            System.out.printf("Победил игрок %d\n", winner);
            dropXOTable();
            return true;
        }
        return false;
    }

    // return 1, 2 or 0, 1,2 - winner, 0 - not winners
    private int determineWinner(int[] xoTableArr){
        // верхняя строка
        if (xoTableArr[0] == xoTableArr[1] && xoTableArr[1] == xoTableArr[2])
            return xoTableArr[0];
        // средняя строка
        if (xoTableArr[3] == xoTableArr[4] && xoTableArr[4] == xoTableArr[5])
            return xoTableArr[3];
        // нижняя строка
        if (xoTableArr[6] == xoTableArr[7] && xoTableArr[7] == xoTableArr[8])
            return xoTableArr[6];
        // левый столбец
        if (xoTableArr[0] == xoTableArr[3] && xoTableArr[3] == xoTableArr[6])
            return xoTableArr[0];
        // средний столбец
        if (xoTableArr[1] == xoTableArr[4] && xoTableArr[4] == xoTableArr[7])
            return xoTableArr[1];
        // правый столбец
        if (xoTableArr[2] == xoTableArr[5] && xoTableArr[5] == xoTableArr[8])
            return xoTableArr[2];
        // диагональ 1
        if (xoTableArr[0] == xoTableArr[4] && xoTableArr[4] == xoTableArr[8])
            return xoTableArr[0];
        // диагональ 2
        if (xoTableArr[2] == xoTableArr[4] && xoTableArr[4] == xoTableArr[6])
            return xoTableArr[2];

        // нет победителей
        return 0;
    }

    private void dropXOTable() throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute("drop table xoTable");
    }

    private ArrayList<Integer> getMoveOptions(int[] xoTableArr) throws SQLException{
        ArrayList<Integer> moveOptions = new ArrayList<Integer>(9);

        for (int i = 0; i < 9; i++){
            // null convert to 0, null => empty cell
            if (xoTableArr[i] == 0)
                moveOptions.add(i);
        }
        return moveOptions;
    }

    private String getXOTableCell(ResultSet xoTable, int index) throws SQLException {
        int playerNumber = xoTable.getInt(index);
        if (playerNumber == 0)
            return "-";
        if (playerNumber == 1)
            return "x";
        return "o";
    }
    private void showXOTable(ResultSet xoTable) throws SQLException {
        System.out.printf(
                "|%s|%s|%s|\n" +
                "|%s|%s|%s|\n" +
                "|%s|%s|%s|\n",
                getXOTableCell(xoTable, 3),
                getXOTableCell(xoTable, 4),
                getXOTableCell(xoTable, 5),
                getXOTableCell(xoTable, 6),
                getXOTableCell(xoTable, 7),
                getXOTableCell(xoTable, 8),
                getXOTableCell(xoTable, 9),
                getXOTableCell(xoTable, 10),
                getXOTableCell(xoTable, 11)
        );
    }

    private ResultSet getXOTable() throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet xoTable;

        try {
            xoTable = statement.executeQuery("select * from xoTable");
            // сразу возьмем строчку с данными, чтобы не забыть
            xoTable.next();
        } catch (SQLException sqlException) {
            xoTable = null;
        }
        return xoTable;
    }

    private boolean isCorrectPlayersNumber(ResultSet xoTable) throws SQLException{
        return xoTable.getInt(1) < 2;
    }

    private void setPlayerNumber() throws SQLException{
        if (playerNumber == 0) {
            playerNumber = 2;
            Statement statement = connection.createStatement();
            statement.execute("update xoTable set playersNumber = 2");
        }
    }

    private ResultSet checkTable() throws SQLException{
        ResultSet xoTable = getXOTable();

        if (xoTable == null)
            xoTable = createXOTable();

        return xoTable;
    }
    private ResultSet createXOTable()  throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute("create table XOTable" +
                "(playersNumber int, playerNumber int," +
                "\"0\" int, \"1\" int, \"2\" int," +
                "\"3\" int, \"4\" int, \"5\" int," +
                "\"6\" int, \"7\" int, \"8\" int)");

        statement.execute("insert into XOTable(playersNumber, playerNumber) values(" +
                "1, 1)");

        playerNumber = 1;
        ResultSet xoTable = getXOTable();
        makeMove(xoTableToArray(xoTable));

        return xoTable;
    }
}