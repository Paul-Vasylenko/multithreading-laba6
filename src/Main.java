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

                MPI.COMM_WORLD.Isend(offset, 0, 1, MPI.INT, dest, 0);
                MPI.COMM_WORLD.Isend(rows, 0, 1, MPI.INT, dest, 1);
                MPI.COMM_WORLD.Isend(a, offset[0], rows[0], MPI.OBJECT, dest, 2);
                MPI.COMM_WORLD.Isend(b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, dest, 3);

                offset[0] = offset[0] + rows[0];
            }
            System.out.println("****Results: ");
            for(int source = 1; source <= numWorkers; source++) {
                var offsetRequest = MPI.COMM_WORLD.Irecv(offset, 0, 1, MPI.INT, source, 4);
                var rowsRequest = MPI.COMM_WORLD.Irecv(rows, 0, 1, MPI.INT, source, 5);
                offsetRequest.Wait();
                rowsRequest.Wait();
                var matrixRequest = MPI.COMM_WORLD.Irecv(c, offset[0], rows[0], MPI.OBJECT, source, 6);
                matrixRequest.Wait();
                System.out.println("Data received by master process");
            }

            for(int i = 0; i < NUMBER_ROWS_A; i++) {
                for (int j = 0; j < NUMBER_COLUMNS_B; j++) {
                    System.out.print(c[i][j] +" ");
                }
                System.out.print('\n');
            }
        } else {
            var offsetRequest = MPI.COMM_WORLD.Irecv(offset, 0, 1, MPI.INT, MASTER, 0);
            var rowsRequest = MPI.COMM_WORLD.Irecv(rows, 0, 1, MPI.INT, MASTER, 1);
            // This data is needed first to get matrices
            offsetRequest.Wait();
            rowsRequest.Wait();
            MPI.COMM_WORLD.Isend(offset, 0, 1, MPI.INT, MASTER, 4);
            MPI.COMM_WORLD.Isend(rows, 0, 1, MPI.INT, MASTER, 5);

            var aRequest = MPI.COMM_WORLD.Irecv(a, 0, rows[0], MPI.OBJECT, MASTER, 2);
            var bRequest = MPI.COMM_WORLD.Irecv(b, 0, NUMBER_COLUMNS_A, MPI.OBJECT, MASTER, 3);
            aRequest.Wait();
            bRequest.Wait();
            System.out.println("Data was received by " + rank + " process");
            for (int k = 0; k < NUMBER_COLUMNS_B; k++) {
                for (int i = 0; i < rows[0]; i++) {
                    for (int j = 0; j < NUMBER_COLUMNS_A; j++) {
                        c[i][k] += a[i][j] * b[j][k];
                    }
                }
            }


            MPI.COMM_WORLD.Isend(c, 0, rows[0], MPI.OBJECT, MASTER, 6);
        }

        MPI.Finalize();
    }
}