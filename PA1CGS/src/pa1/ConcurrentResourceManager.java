//Cassandra Morales
// CGS 3763
// PA1
// 10/26/25

package pa1;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ConcurrentResourceManager {

    private static final int MAX = 5;

    private static int randomInRange(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    // pauses between threads 
    private static void Sleep(int microseconds) {
        try {
            if (microseconds <= 0) {
                return;
            }
            long micros = microseconds / 1000;
            int nanos  = (microseconds % 1000) * 1000;
            Thread.sleep(micros, nanos);
        } 
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }


    // resource manager that is not thread-safe (may cause race conditions)
    static class Naive {
    	
    	// how many resources available
        private int free = MAX;
        
        // takes resources and decreases available resources count
        int acquire(int n) {
        	
        	// if no resources available
            if (free < n) {
            	return -1;
            }
            
            int before = free;
            free -= n;
            
            System.out.printf("Naive taking=%d, before=%d, after=%d%n", n, before, free);
            
            return 0;
        }
        
        // releases the resources again 
        int release(int n) {
        	
            int before = free;
            free += n;
            
            // makes sure the amount of resources isnt more than the max
            if (free > MAX) {
            	free = MAX;
            }
            
            System.out.printf("Naive giving=%d, before=%d, after=%d%n", n, before, free);
            
            return 0;
        }
        
        // returns available resources
        int free() { 
        	return free; 
        }
    }

    //Resource manager that is thread safe using a mutex lock to prevent race conditions
    static class Locked {
    	
        private int free = MAX;
        
        // creating lock 
        private final ReentrantLock m = new ReentrantLock();
        
        int acquire(int n) {
        	
        	//locks access so only one thread can enter at a time
            m.lock();
            
            
            try {
            	// if not enough resources
                if (free < n) {
                	return -1;
                }
                
                // sets before to the max
                int before = free;
                
                // takes the resources
                free -= n;
                
                System.out.printf("lock takes=%d, before=%d, after=%d%n", n, before, free);
                
                return 0;
            } 
            
            //unlocks for the next thread
            finally {
                m.unlock();
            }
        }
        
        int release(int n) {
        	
        	// locks m lock so only one thread can enter at a time
            m.lock();
            
            try {
            	// before set to the max amount of resources
                int before = free;
                
                // adds the number of resources you want
                free += n;
                
                // if greater than the max possible resets the value
                if (free > MAX) {
                	free = MAX;
                }
                
                System.out.printf("Lock giving=%d, before=%d, after=%d%n", n, before, free);
                
                return 0;
            } 
            
            //unlock for the next thread to use
            finally {
                m.unlock();
            }
        }
        
        // resets resources
        void clear() {
        	
        	//locks so one can enter at a time
            m.lock();
            
            // sets full capacity
            try { 
            	free = MAX; 
            }
            
            // unlocks for other threads to use
            finally { 
            	m.unlock(); 
            }
        }
    }

    // resource manager that waits to take larger amount of resources
    static class Wait {
    	
        private int free = MAX;
        private final ReentrantLock m = new ReentrantLock();
        private final Condition hold = m.newCondition();
        
        
        void acquire(int n) throws InterruptedException {
        	
        	// locks for 1 thread at a time
            m.lock();
            
            try {
            	// while not enough resources
                while (free < n) {
                	// wait but releases lock while waiting, relocks once there are enough resources
                	hold.await();
                }
                
                // set to max
                int before = free;
                
                // once theres enough resources, subtract the num of resources you want from the ones available 
                free -= n;
                
                System.out.printf("wait takes=%d, before=%d, after=%d%n", n, before, free);
            } 
            // unlocks for next thread
            finally {
                m.unlock();
            }
        }
        
        void release(int n) {
        	
        	// prevents race conditioning
            m.lock();
            
            try {
            	// set to max
                int before = free;
                
                // add how many resources you want to use
                free += n;
                
                // if more than the max then resets value
                if (free > MAX) {
                	free = MAX;
                }
                
                System.out.printf("wait giving=%d, before=%d, after=%d%n", n, before, free);
                
                // all threads waiting for there to be available resources check again
                hold.signalAll();
            } 
            
            //unlock for the next thread
            finally {
                m.unlock();
            }
        }
        
        
        int free() {
        	
        	//locks for one thread at a time
            m.lock();
            
            // tells you how many available resources
            try { 
            	return free; 
            }
            
            finally { 
            	//unlocks for more threads to use
            	m.unlock(); 
            }
        }
    }

    // monitor resource manager that reduces starvation of the waiting threads
    static class Guarded {
    	
        private int free = MAX;
        private final ReentrantLock m = new ReentrantLock(true);
        private final Condition hold = m.newCondition();
        
        void acquire(int n) throws InterruptedException {
        	
        	//locks, 1 thread at a time
            m.lock();
            
            try {
            	
            	// while not enough resources
                while (free < n) {
                	
                	// waits for more resources and allows other threads to enter
                	hold.await();
                }
                
                // sets to max
                int before = free;
                
                // subtracts the amount of resources you want
                free -= n;
                
                System.out.printf("monitor takes=%d, before=%d, after=%d%n", n, before, free);
            } 
            finally {
            	// unlocks for next thread
                m.unlock();
            }
        }
        
        
        void release(int n) {
        	//one at a time
            m.lock();
            try {
            	//set to max
                int before = free;
                //add num of resources wanted
                free += n;
                
                // if more than available then resets the value
                if (free > MAX) {
                	free = MAX;
                }
                System.out.printf("monitor gives=%d, before=%d, after=%d%n", n, before, free);
                
                //all waiting threads check if theres enought available threads now
                hold.signalAll();
            } 
            finally {
            	//unlocks for the next thread
                m.unlock();
            }
        }
        
        // returns the amount of free resources and lets next thread go through
        int free() {
            m.lock();
            try { 
            	return free; 
            }
            finally { 
            	m.unlock(); 
            }
        }
    }

    // testing
    static class Task implements Runnable {
    	
    	// how many times the thread will loop
        private final int reps;
        // makes threads wait if not using monitor
        private final Wait wp;
        // makes threads uses guarded is using monitor
        private final Guarded gp;
        // using monitor or not
        private final boolean useMon;
        // waiting for all threads to finish
        private final CountDownLatch done;
        
        
        // manages what threads are supposed to do
        Task(int reps, Wait wp, Guarded gp, boolean useMon, CountDownLatch done) {
            this.reps = reps; 
            this.wp = wp; 
            this.gp = gp; 
            this.useMon = useMon; 
            this.done = done;
        }
        
        public void run() {
        	
            try {
            	// loops for resource sharing
                for (int i = 0; i < reps; i++) {
                	// 
                    int need = randomInRange(1, 3);
                    
                    try {
                    	// wait until enough resources are free using guarded or wait depending on monitor 
                        if (useMon) {
                        	gp.acquire(need);
                        }
                        else {
                        	wp.acquire(need);
                        }
                    } 
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    
                    // pauses between threads
                    Sleep(randomInRange(1000, 8000));
                    
                    // uses monitor, 1-3 so threads dont ask for toooooo many resources
                    if (useMon) {
                        gp.release(randomInRange(1, 3));
                    } else {
                        wp.release(randomInRange(1, 3));
                    }
                    
                    // pauses before the next loop
                    Sleep(randomInRange(1000, 8000));
                }
            } 
            
            // decrements
            finally {
                done.countDown();
            }
        }
    }

    // testing 
    private static void sample() {
    	
        System.out.println("naive");
        
        Naive a = new Naive();
        a.acquire(3);
        a.release(2);
        a.acquire(4);
        
        System.out.printf("Naive ends free=%d%n", a.free());
        
        System.out.println("\nlocked");
        
        Locked b = new Locked();
        b.acquire(3);
        b.release(2);
        
        if (b.acquire(4) != 0) {
        	System.out.println("Lock not enough for 4");
        }
        
        b.clear();
    }

    // concurrency test
    private static void runWait(int n, int it) throws InterruptedException {
    	
        System.out.println("\nwait");
        
        Wait pool = new Wait();
        ExecutorService ex = Executors.newFixedThreadPool(n);
        CountDownLatch cd = new CountDownLatch(n);
        
        for (int i = 0; i < n; i++) {
        	ex.execute(new Task(it, pool, null, false, cd));
        }
        
        cd.await();
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.printf("Wait ends free=%d%n", pool.free());
    }

    // concurrency test
    private static void runMon(int n, int it) throws InterruptedException {
    	
        System.out.println("\nmonitor");
        
        Guarded mon = new Guarded();
        ExecutorService ex = Executors.newFixedThreadPool(n);
        CountDownLatch cd = new CountDownLatch(n);
        
        for (int i = 0; i < n; i++) {
        	ex.execute(new Task(it, null, mon, true, cd));
        }
        
        cd.await();
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);
        
        System.out.printf("Monitor ends free=%d%n", mon.free());
    }

    public static void main(String[] args) throws Exception {
    	
        boolean onlyWait = args.length > 0 && "waiting".equals(args[0]);
        boolean onlyMon  = args.length > 0 && "monitor".equals(args[0]);
        
        sample();
        
        int threads = 8;
        int iter = 20;
        
        if (!onlyMon) {
        	runWait(threads, iter);
        }
        
        if (!onlyWait) {
        	runMon(threads, iter);
        }
        
        System.out.println("\nDone.");
    }
}





