import mpi.MPI;

public class Main {
    private static final int NUMBER_ROWS_A = 4;
    private static final int NUMBER_COLUMNS_A = 4;
    private static final int NUMBER_COLUMNS_B = 4;
    private static final int MASTER = 0;
    private static final int FROM_MASTER = 1;
    private static final int TO_MASTER = 2;
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
        int numWorkers = numTasks - 1;
        int[] offset = {0};
        int[] rows = {0};
        if(rank == MASTER) {
            System.out.println("MPI started with " + numTasks + " tasks");

            for(int i = 0; i < NUMBER_ROWS_A; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_A; j++) {
                    a[i][j] = 10;
                }
            }
            for(int i = 0; i < NUMBER_COLUMNS_A; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_B; j++) {
                    b[i][j] = 10;
                }
            }

            int rowsPerThread = NUMBER_ROWS_A / numWorkers;
            int extra = NUMBER_ROWS_A % numWorkers;

            for(int dest = 1; dest <= numWorkers; dest++) {
                rows[0] = (dest <= extra) ? rowsPerThread + 1 : rowsPerThread;
                System.out.println("Sending " + rows[0] + " rows to task " + dest + " offset="+offset[0]);

                MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, dest, FROM_MASTER);
                MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, dest, FROM_MASTER);
                MPI.COMM_WORLD.Send(a, offset[0], rows[0], MPI.OBJECT, dest, FROM_MASTER);
                MPI.COMM_WORLD.Send(b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, dest, FROM_MASTER);

                offset[0] = offset[0] + rows[0];
            }
            System.out.println("****Results: ");
            for(int source = 1; source <= numWorkers; source++) {
                MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, TO_MASTER);
                MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, TO_MASTER);
                MPI.COMM_WORLD.Recv(c, offset[0], rows[0], MPI.OBJECT, source, TO_MASTER);
            }

            for(int i = 0; i < NUMBER_ROWS_A; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_B; j++) {
                    System.out.print(c[i][j] +" ");
                }
                System.out.print('\n');
            }
        } else {
            MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, MASTER, FROM_MASTER);
            MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, MASTER, FROM_MASTER);
            MPI.COMM_WORLD.Recv(a, 0, rows[0], MPI.OBJECT, MASTER, FROM_MASTER);
            MPI.COMM_WORLD.Recv(b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, MASTER, FROM_MASTER);

            System.out.println("Data was received by " + rank + " process");

            for (int k = 0; k < NUMBER_COLUMNS_B; k++) {
                for (int i = 0; i < rows[0]; i++) {
                    for (int j = 0; j < NUMBER_COLUMNS_A; j++) {
                        c[i][k] += a[i][j] * b[j][k];
                    }
                }
            }

            MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, MASTER, TO_MASTER);
            MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, MASTER, TO_MASTER);
            MPI.COMM_WORLD.Send(c, 0, rows[0], MPI.OBJECT, MASTER, TO_MASTER);
        }

        MPI.Finalize();
    }
}