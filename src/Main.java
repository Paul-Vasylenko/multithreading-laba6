import mpi.MPI;

public class Main {
    private static final int NUMBER_ROWS_A = 2000;
    private static final int NUMBER_COLUMNS_A = 2000;
    private static final int NUMBER_COLUMNS_B = 2000;
    private static final int MASTER = 0;
    public static void main(String[] args) {
        double[][] a = new double[NUMBER_ROWS_A][NUMBER_COLUMNS_A];
        double[][] b = new double[NUMBER_COLUMNS_A][NUMBER_COLUMNS_B];
        double[][] c = new double[NUMBER_ROWS_A][NUMBER_COLUMNS_B];
        MPI.Init(args);
        int numTasks = MPI.COMM_WORLD.Size();
        int rank = MPI.COMM_WORLD.Rank();

        if (numTasks < 2) {
            System.out.println("Need at least two MPI tasks. Quitting...\n");
            MPI.COMM_WORLD.Abort(1);
        }
        long start = 0;
        if(rank == MASTER) {
            for (int i = 0; i < NUMBER_ROWS_A; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_A; j++) {
                    a[i][j] = i*2 + j;
                }
            }
            for (int i = 0; i < NUMBER_COLUMNS_A; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_B; j++) {
                    b[i][j] = j*2+i;
                }
            }
            start = System.currentTimeMillis();
        }
        int rowsPerThread = NUMBER_ROWS_A / numTasks;
        int extra = NUMBER_ROWS_A % numTasks;

        int[] rowsCounts = new int[numTasks];
        int[] offsetCounts = new int[numTasks];
        for(int i = 0; i < numTasks; i++) {
            rowsCounts[i] = i < extra ? rowsPerThread + 1 : rowsPerThread;
            offsetCounts[i] = i == MASTER ? 0 : offsetCounts[i-1] + rowsCounts[i-1];
        }

        int rowsForThisTask = rowsCounts[rank];
        double[][] aRows = new double[rowsForThisTask][NUMBER_COLUMNS_A];
        // Send rows of matrix A
        MPI.COMM_WORLD.Scatterv(
                a, 0, rowsCounts, offsetCounts,MPI.OBJECT,
                aRows, 0, rowsForThisTask, MPI.OBJECT,
                MASTER
        );
        // Send all matrix B
        MPI.COMM_WORLD.Bcast(b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, MASTER);

        double[][] cRows = new double[rowsForThisTask][NUMBER_COLUMNS_B];
        for (int k = 0; k < NUMBER_COLUMNS_B; k++) {
            for (int i = 0; i < rowsForThisTask; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_A; j++) {
                    cRows[i][k] += aRows[i][j] * b[j][k];
                }
            }
        }
//        System.out.println(rank + " : " + Arrays.toString(cRows[0]));

        MPI.COMM_WORLD.Gatherv(
                cRows, 0, rowsForThisTask, MPI.OBJECT,
                c, 0, rowsCounts, offsetCounts, MPI.OBJECT,
                MASTER
        );

        if (rank == MASTER) {
            var endTime = System.currentTimeMillis();
            var dur = endTime - start;

//            for(int i = 0; i < NUMBER_ROWS_A; i++) {
//                for (int j = 0; j < NUMBER_COLUMNS_B; j++) {
//                    System.out.print(c[i][j] +" ");
//                }
//                System.out.print('\n');
//            }
            System.out.println("time: " + dur);
        }
        MPI.Finalize();
    }
}