
/*
 ******************************************************************************
 *
 *  @Program:       Project2.java
 *  @Author:        Nisha P. Chandra
 *  @Class:         CS 4348.002 - Operating systems concepts
 *  @Instructor:    Greg Ozbirn
 * 
 *  @Description:   This program simulates a Movie Theater ticketing system
 *                  with multiple actors - Customers, Box Office agents, Ticket 
 *                  takers/checkers, Concession stand workers etc.  This 
 *                  program relies on threads to handle synchronization of the
 *                  ticket purchase sequence by a customer for a select
 *                  number of total seats available at the theater complex for
 *                  each of the movies that are showing at the complex.  Each
 *                  customer is assigned to a separate thread just like the
 *                  box office agents, ticket taker and concession worker are
 *                  assigned to other separate threads.
 *  
 *                  Semaphores keep the threads from trying to acquire
 *                  shared resources and carrying out critical transactions 
 *                  simultaneously which could lead to wrong results/outcomes 
 *                  and other problems associated with multithreading including 
 *                  program crashes
 *                 
 * 
 *  @Due Date:      April 30, 2022 
 *                
 ********************************************************************************
 */

// imports here!
import java.util.*;
import java.util.concurrent.Semaphore;
import java.io.*;


public class Project2
{
    // Constants here
    //
    public static final int CUSTOMERS = 50;
    public static final int BOXOFFICEAGENTS = 2;
    public static final int CONCESSIONS = 3;
    
    // Class variables here
    //
    private static int [] datMovieSeats;
    private static String [] datMovieTitles;
    private static int datTicketRequestCustomer[] = new int[2];
    private static int datTicketRequestMovie[] = new int[2];
    private static int datTicketRequestStatus[] = new int[2];
    private static int datTicketTakerRequestCustomer;
    private static int datFoodPurchase;
    private static int datFoodPurchaseCustomer;
    public static Semaphore semTheaterLine = new Semaphore ( 0, true );
    public static Semaphore semBOACounter[] = { new Semaphore( 0, true ), 
                                                new Semaphore ( 0, true ) };
	public static Semaphore semTicketRequest[] = {  new Semaphore ( 0, true ), 
                                                    new Semaphore ( 0, true ) };
	public static Semaphore semTicketRequestStatus[] = {new Semaphore ( 0, true ), 
                                                        new Semaphore ( 0, true ) };
	public static Semaphore semTicketTakerLine = new Semaphore ( 0, true );
	public static Semaphore semTicketTakerRequest = new Semaphore ( 0, true );
	public static Semaphore semTicketTakerRequestStatus = new Semaphore ( 0, true );
	public static Semaphore semConcessionStandWorkerLine = new Semaphore ( 0, true );
	public static Semaphore semConcessionRequest = new Semaphore ( 0, true );
	public static Semaphore semConcessionRequestStatus = new Semaphore ( 0, true );

    // Random variable
    static Random rand = new Random ();

    // Main thread
    //
    public static void main ( String[] args ) throws IOException
    {

        // Parse input file
        //
        File inputFile = null;
        int totalLines =  0;

        // Input filename (movies.txt)
        // is passed in as an argument
        // to this program
        //
        if ( args.length != 1 )
        {
            System.out.println ( "Wrong number of Argument(s)." );
            System.out.println ( "Usage: java CPU <input file name>" );
            System.out.println ( "Exiting...." );
            System.exit ( 0 );
        }

        // Command line OK.
        //
        else 
        {
            inputFile = new File ( args [0] );
        
            // Check to make sure that the input
            // file exists.
            if ( !inputFile.exists() )
            {
                System.out.println ( "File not found, Exiting!" );
                System.exit ( 0 );
            }

            else
            {
                // Each line in the text file
                // contains a movie title and number of seats
                //
                try ( BufferedReader reader = new BufferedReader ( new FileReader ( inputFile )) )
                { 
                    while ( reader.readLine() != null ) 
                            totalLines++;

                    datMovieTitles = new String [totalLines];
                    datMovieSeats = new int [totalLines];
                    reader.close ();
                }

                catch ( IOException e )
                {
                    e.printStackTrace ();
                }
            }
        }

        // Copy movies and seat out from input file
        //
        String lineSplit[];
		try
        {	
			Scanner sc = new Scanner ( inputFile );
			String line;
			int i = 0;
			
            // Split on tab
            //
			while ( sc.hasNextLine() )
            {
				line = sc.nextLine ();
				lineSplit = line.split ( "\t" );
				datMovieTitles[i] = lineSplit[0];
				datMovieSeats[i] = Integer.parseInt ( lineSplit [1] );
				i++;
			}
            sc.close();
        }
	
        // Catch any exceptions and error
        // out. Exit here if something wrong
        //
        catch ( Exception e )
        {
            e.printStackTrace ();
            System.exit ( -1 );
		}

        // Each of the customer and box agent objects is run 
        // in a separate thread.  Create all of the threads 
        // and start them.
        // Box office agent
        //
        Thread boxOfficeAgent[] = new Thread[BOXOFFICEAGENTS];
        for ( int j = 0; j < BOXOFFICEAGENTS; j++)
        {
            boxOfficeAgent[j] = new Thread ( new BoxOfficeAgent (j) );
            boxOfficeAgent[j].start ();
        }

        // Ticket taker
        //
        Thread ticketTaker = new Thread ( new TicketTaker () );
        ticketTaker.start();

        // Concession stand worker
        //

        Thread concessionStandWorker = new Thread  ( new ConcessionStandWorker () );
        concessionStandWorker.start();

        // Customers
        //
        Thread customer[] = new Thread[CUSTOMERS];
        for ( int i = 0; i < CUSTOMERS; i++ )
        {
            customer[i] = new Thread ( new Customer ( i ));
            customer[i].start();
        }

        System.out.println( "Theater is open" );

        // Join all customer threads. Not sure
        // whether this will make main () execute
        // until all threads are done.
        //
		for ( int i = 0; i < CUSTOMERS; i++ )
    	{
    		try 
            {
				customer[i].join();
				System.out.println ( "Joined customer " + i );
			} 
            
            catch ( InterruptedException e ) 
            {
                e.printStackTrace ();
            }
    	}

        // Exit when all the threads are done
        //
        System.exit ( 0 );
    }
    // end main ()

    /*
    *******************************************************************************
    *
    *   @Class:         Customer
    *   @Description:   This class implements the "Runnable" interface by over-
    *                   riding the run () method.
    *                
    ********************************************************************************
    */

    // Class Customer implements Runnable interface
    //
    public static class Customer implements Runnable
    {
        // Class variables here
        //
        private int custID;
        // Each customer has an ID that
        // is used for all threater
        // transactions
        //
        public Customer ( int custID )
        {
            this.custID = custID;
        }

        @Override
        public void run ()
        {
            // Select movie 
            //   
            int movieID = selectMovie ();
            System.out.println ( "Customer " + this.custID + " created, buying ticket to " + datMovieTitles[movieID] );

            // Get in theater line to wait
            // on a free counter.
            //
            this.semWait ( semTheaterLine );
            int boxOfficeCounter = this.findFreeBoxOfficeCounter ();

            // Approach the free box office counter
            // and request purchase of ticket for
            // selected movie.
            // 
            this.purchaseMovieTicket ( boxOfficeCounter, movieID );

            // Signal to the box office agent 
            //
            this.semSignal ( semTicketRequest[boxOfficeCounter] );

            // Now wait for the box office agent to process
            // ticket purchase request.
            //
            this.semWait ( semTicketRequestStatus[boxOfficeCounter] );

            // If ticket transaction is successful, then
            // get into the movie theater, meet ticket taker and
            // visit concessions before watching movie...
            //
            if ( this.isTicketAvailable ( boxOfficeCounter ) )
            {
                // Get in the ticket taker line
                // and wait for the ticket taker
                //
                System.out.println ( "Customer " + this.custID + " in line to see ticket taker" );
                this.semWait ( semTicketTakerLine );

                // Signal to the ticket taker
                // to accept movie ticket and wait
                // for ticket taker
                //
                datTicketTakerRequestCustomer = this.custID;
                this.semSignal ( semTicketTakerRequest );
                this.semWait ( semTicketTakerRequestStatus );

                // Is customer visiting concessions?
                //
                if ( this.isCustomerVisitingConcessions () )
                {
                    // Find out what customer wants to purchase
                    // and get in concession line and wait for 
                    // order to be taken
                    //
                    int concessionPurchased = customerFoodPurchase ();
                    String foodPurchased = customerFoodPurchased ( concessionPurchased );
                    this.semWait ( semConcessionStandWorkerLine );
                    System.out.println ( "Customer " + this.custID + " in line to buy "
                                            + foodPurchased );
                    
                    // Pass order to concession worker
                    // and wait for the food order to be
                    // fulfilled
                    //
                    this.purchaseFood ( concessionPurchased );
                    this.semSignal ( semConcessionRequest );
                    this.semWait ( semConcessionRequestStatus );
                    System.out.println ( "Customer " + this.custID + " receives " + foodPurchased );
                }
                // Enter theater to see the movie
                System.out.println ( "Customer " + this.custID + " enters theater to see " + datMovieTitles[movieID] );
            }
            // ...else leave the theater
            //
            else
            {
                System.out.println ( "Customer " + this.custID + " was not successful at obtaining ticket for " 
                                        + datMovieTitles[movieID] + ", leaving theater!" );
            }
        }

        // Method selectMovie () to select movie 
        // for ticket purchase 
        //
        private int selectMovie () 
        {
            int movieID;

            // Movie is chosen randomly from the array
            // of movies showing at the theater
            //
            movieID = rand.nextInt ( datMovieTitles.length );
            return movieID;
        }

        // Method semWait () acquires a semaphore to keep access to shared variables and 
        // critical transactions separate/unique among all threads. The current 
        // thread WAITS until it can successfully acquire the semaphore.
        // 
        private void semWait ( Semaphore sem )
        {
            // Block until semaphore can be acquired
            //
            try
            {
                sem.acquire ();
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace ();
            }
        }

        // Method semSignal () releases a semaphore to signal release of a
        // shared variable or completion of a critical transaction.
        // 
        private void semSignal ( Semaphore sem )
        {
            // Release the semaphore
            //
            sem.release ();
        }

        // Figure out which box office counter
        // is free to accept the ticket purchase
        // request
        //
        private int findFreeBoxOfficeCounter ()
        {
            int boaID;
            // Try acquiring one of the two counters
            // because one of them could be used by other
            // threads.
            //
            if ( ! semBOACounter[0].tryAcquire () )
            {

                this.semWait ( semBOACounter[1] );
                boaID = 1;
            }
                //return 1;
            else 
                boaID = 0;
            return boaID;
        }

        // Purchase ticket at the box office
        //
        private void purchaseMovieTicket ( int boxOfficeCounter, int movieID )
        {
            datTicketRequestCustomer[boxOfficeCounter] = this.custID;
            datTicketRequestMovie[boxOfficeCounter] = movieID;
        }

        // Check whether ticket purchase
        // was successful
        private boolean isTicketAvailable ( int boxOfficeCounter )
        {
            if ( datTicketRequestStatus[boxOfficeCounter] != -1 )
                return true;
            else
                return false;
        }

        // Check whether customer is visiting
        // the conncession stand
        private boolean isCustomerVisitingConcessions ()
        {
            // 50% chance to visit concession stand
            //
            if ( rand.nextBoolean () )
                return true;
            
            else
                return false;
        }

        // Purchase food
        //
        private int customerFoodPurchase ()
        {
            // Randomly chosen from the 
            // food choices: 
            //  1. Popcorn, 2. Soda
            //  3. Popcorn and Soda
            //
            return rand.nextInt ( CONCESSIONS );
        }

        // Food purchased by customer
        //
        private String customerFoodPurchased ( int food )
        {
            String foodName = null;

            // What did the customer purchase
            //
            switch ( food )
            {    
                case 0:    
                    foodName = "Popcorn";
                    break;

                case 1:    
                    foodName = "Soda";
                    break;

                case 2:
                    foodName = "Popcorn and Soda";
                    break;  
            }
            return foodName;    
        }

        // Purchase food from concessions
        //
        private void purchaseFood ( int concessionPurchased )
        {
            datFoodPurchaseCustomer = this.custID;
            datFoodPurchase = concessionPurchased;
        }
    
    }

    // end Customer class

    /*
     *******************************************************************************
     *
     *   @Class:         BoxOfficeAgent
     *   @Description:   This class implements the "Runnable" interface by over-
     *                   riding the run () method.
     *                
     ********************************************************************************
     */

     // Class BoxOfficeAgent implements Runnable interface
     //
    public static class BoxOfficeAgent implements Runnable
    {
        // Class variables here
        //
        private int boaID;

        // Constructor
        //
        public BoxOfficeAgent ( int boaID )
        {
            this.boaID = boaID;
        }

        @Override
        public void run ()
        {
            // Create box office agent threads and process
            // customer ticket transactions. Start accepting
            // customers when the box office is open
            //
            System.out.println ( "Box office agent " + this.boaID + " created" );
            while ( true )
            {
                // Signal for next customer and 
                // wait for ticket request
                //
                this.semSignal ( semBOACounter[boaID] );
                this.semSignal ( semTheaterLine );
                this.semWait ( semTicketRequest[boaID] );
                System.out.println ( "Box office agent " + this.boaID + " serving customer " 
                                    + datTicketRequestCustomer[boaID] );

                // Process ticket request
                //
                if ( this.processTicketRequest ( datTicketRequestMovie[boaID] ) )
                {
                    System.out.println ( "Box office agent " + this.boaID + " sold ticket for "
                                           + datMovieTitles[datTicketRequestMovie[boaID]] + " to customer " + datTicketRequestCustomer[boaID] );
                }

                // Ticket processing completed
                //
                this.semSignal ( semTicketRequestStatus[boaID] );
            }
        }

        // Method semWait () acquires a semaphore to keep access to shared variables and 
        // critical transactions separate/unique among all threads. The current 
        // thread WAITS until it can successfully acquire the semaphore.
        // 
        private void semWait ( Semaphore sem )
        {
            // Block until semaphore can be acquired
            //
            try
            {
                sem.acquire ();
            }

            catch ( InterruptedException e )
            {
                e.printStackTrace ();
            }
        }

        // Method semSignal () releases a semaphore to signal release of a
        // shared variable or completion of a critical transaction.
        // 
        private void semSignal ( Semaphore sem )
        {
            // Release the semaphore
            //
            sem.release ();
        }

        // Process ticket request from customer
        //
        private boolean processTicketRequest ( int movieID )
        {

            // Ticket agent processing time
            // is simulated by sleeping this
            // box office agent thread for 1500ms
            // (1/60) of 90 seconds = 1500 millisec
            //
            try 
            {
               Thread.sleep ( 1500 );
            }
            catch (InterruptedException e) 
            {
               e.printStackTrace();
            }

            // Figure out whether seats to 
            // requested movie available
            //
            if ( datMovieSeats[movieID] > 0 )
            {
                datMovieSeats[movieID]--;
                return true;
            }

            else
            {
                datTicketRequestStatus[boaID] = -1;
                return false;
            }
        }
    }

    // end BoxOfficeAgent class

    /*
     *******************************************************************************
     *
     *   @Class:         TicketTaker
     *   @Description:   This class implements the "Runnable" interface by over-
     *                   riding the run () method.
     *                
     ********************************************************************************
     */

    // Class TicketTaker implements Runnable interface
    //
    public static class TicketTaker implements Runnable
    {
        @Override
        public void run ()
        {
            // Ticket taker will verify movie ticket
            // 
            System.out.println ( "Ticket taker created" );
            while ( true )
            {
                // Signal to next customer in line
                // and wait for request
                //
                this.semSignal ( semTicketTakerLine );
                this.semWait ( semTicketTakerRequest );

                // Ticket taker processing time
                // is simulated by sleeping this
                // ticket taker thread for 250ms
                // (1/60) of 15 seconds = 250 millisec
                //
                System.out.println ( "Ticket taken from customer " + datTicketTakerRequestCustomer );
                try 
                {
                   Thread.sleep ( 250 );
                }
                catch (InterruptedException e) 
                {
                   e.printStackTrace();
                }

               // Ticket taker processing done
               //
               this.semSignal ( semTicketTakerRequestStatus );
            }
        }

        // Method semWait () acquires a semaphore to keep access to shared variables and 
        // critical transactions separate/unique among all threads. The current 
        // thread WAITS until it can successfully acquire the semaphore.
        // 
        private void semWait ( Semaphore sem )
        {
            // Block until semaphore can be acquired
            //
            try
            {
                sem.acquire ();
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace ();
            }
        }

        // Method semSignal () releases a semaphore to signal release of a
        // shared variable or completion of a critical transaction.
        // 
        private void semSignal ( Semaphore sem )
        {
            // Release the semaphore
            //
            sem.release ();
        }
    }

    // end TicketTaker Class


    /*
    *******************************************************************************
     *
     *   @Class:         ConcessionStandWorker
     *   @Description:   This class implements the "Runnable" interface by over-
     *                   riding the run () method.
     *                
     ********************************************************************************
    */

    // Class ConcessionStandWorker implements Runnable interface
    //
    public static class ConcessionStandWorker implements Runnable
    {
        @Override
        public void run ()
        {
            // Concession stand worker will 
            // will process concession/food requests
            // from customers
            // 
            System.out.println ( "Concession stand worker created" );
            while ( true )
            {
                // Signal to next customer in line
                // and wait for request
                //
                this.semSignal ( semConcessionStandWorkerLine );
                this.semWait ( semConcessionRequest );

                // Concession stand processing time
                // is simulated by sleeping this
                // concession stand worker thread for 3000ms
                // (1/60) of 180 seconds = 3000 millisec
                //
                System.out.println ( "Order for " + customerFoodPurchased ( datFoodPurchase ) + " taken from customer "  
                                    + datFoodPurchaseCustomer );
                try 
                {
                    Thread.sleep ( 3000 );
                }
                catch (InterruptedException e) 
                {
                    e.printStackTrace();
                }

               // Ticket taker processing done
               //
               System.out.println ( customerFoodPurchased ( datFoodPurchase ) + " given to customer "  
                                    + datFoodPurchaseCustomer );               
               this.semSignal ( semConcessionRequestStatus );
            }
        }

        // Method semWait () acquires a semaphore to keep access to shared variables and 
        // critical transactions separate/unique among all threads. The current 
        // thread WAITS until it can successfully acquire the semaphore.
        // 
        private void semWait ( Semaphore sem )
        {
           // Block until semaphore can be acquired
           //
           try
           {
               sem.acquire ();
           }
    
           catch ( InterruptedException e )
           {
               e.printStackTrace ();
           }
        }

        // Method semSignal () releases a semaphore to signal release of a
        // shared variable or completion of a critical transaction.
        // 
        private void semSignal ( Semaphore sem )
        {
            // Release the semaphore
            //
            sem.release ();
        }

        // Food purchased by customer
        //
        private String customerFoodPurchased ( int foodID )
        {
            String foodName = null;

            // What did the customer purchase
            //
            switch ( foodID )
            {    
                case 0:    
                    foodName = "Popcorn";
                    break;

                case 1:    
                    foodName = "Soda";
                    break;

                case 2:
                    foodName = "Popcorn and Soda";
                    break;  
            }
            return foodName;    
        }
    }

    // end ConcessionStandWorker Class


}
// end Project2 class
