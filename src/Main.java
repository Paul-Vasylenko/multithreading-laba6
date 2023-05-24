import mpi.*;

public class Main {

    public static void main(String args[]) throws Exception {
        MPI.Init(args);
        int me = MPI.COMM_WORLD.Rank();
        System.out.println("Hi from <"+me+">");
        MPI.Finalize();
    }
}