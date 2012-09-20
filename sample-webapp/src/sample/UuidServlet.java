package sample;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
// Stax API:
import javax.xml.stream.XMLStreamException;

// StaxMate:
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.in.*;
import org.codehaus.staxmate.out.*;

// Java Uuid Generator:
import org.safehaus.uuid.EthernetAddress; // for time+location based method
import org.safehaus.uuid.UUID;
import org.safehaus.uuid.UUIDGenerator;

/**
 * This is the simple UUID servlet, implemented using StaxMate XML
 * library and JUG Uuid Generator.
 *<p>
 * POST and GET implementations are different just to demonstrate
 * that it's easy to allow both query-parameter (GET)
 * and XML-document (POST) approaches to implementing REST (aka
 * Plain Old Xml == POX) services. And of course, how easy it
 * is to both write and read xml using StaxMate.
 *<p>
 * Here's sample xml request format:
 *<pre>
 *  &lt;request>
 *   &lt;generate-uuid method="RANDOM" />
 *   &lt;generate-uuid method="LOCATION" count="3" />
 *   &lt;generate-uuid method="NAME">http://www.cowtowncoder.com/foo&lt;/generate-uuid>
 *  &lt;/request>
 *</pre>
 *<p>
 * And here's a sample response, for given request:
 *<pre>
 *  &lt;response>
 *   &lt;uuid>&lt;/uuid>
 *  &lt;/respone>
 *</pre>
 *<p>
 * Additionally, query interface recognizes following parameters:
 * <ul>
 *  <li>method: same as method attribute
 *   </li>
 *  <li>count: same as count attribute
 *   </li>
 *  <li>name: argument used with method 'name'
 *   </li>
 *  </ul>
 */
@SuppressWarnings("serial")
public class UuidServlet
    extends HttpServlet
{
    /**
     * Enumeration used to define UUID generation types
     */
    enum UUIDMethod {
        RANDOM, TIME, NAME
    }

    /**
     * Let's limit total number of UUIDs we will return for any
     * request, just as a simple protection against greedy clients
     * (and to demonstrate one way to indicate such extra info to caller)
     */
    final static int MAX_UUIDS_PER_REQUEST = 100;

    /**
     * This constant determines amount of artificial delay (in milliseconds)
     * to add
     * before sending the reply. Value of 0 or below denotes "no delay".
     * Delay can be added to experiment with effects of high latency
     * requests to the throughput.
     */
    //final static int REPLY_DELAY = 100;
    final static int REPLY_DELAY = 0;

    /**
     * Could require Ethernet address to be passed, or could use
     * JNI-based access: but for now, let's just generate a
     * dummy (multicast) address.
     */
    final EthernetAddress mMacAddress;

    // // // Statistics:

    /**
     * Flag to set to enable keeping of metrics
     */
    volatile boolean mCfgUpdateMetrics = true;

    /**
     * Total number of requests served since the server started
     */
    int mRequestsServed = 0;

    /**
     * Total number of requests of which service method is active
     * (entered, not yet exited)
     */
    int mActiveRequests = 0;

    MyMetrics mMetrics = null;

    /**
     * For efficiency we should share this factory with all the
     * threads: factories are thread-safe after construction.
     */
    SMInputFactory mSmInFactory;

    /**
     * For efficiency we should share this factory with all the
     * threads: factories are thread-safe after construction.
     */
    SMOutputFactory mSmOutFactory;

    public UuidServlet() {
        mMacAddress = UUIDGenerator.getInstance().getDummyAddress();
    }

    @Override
    public void init(ServletConfig cfg)
    {
        mSmInFactory = SMInputFactory.getGlobalSMInputFactory();
        mSmOutFactory = SMOutputFactory.getGlobalSMOutputFactory();
        // Independent of whether metrics are enabled, let's start thread
        mMetrics = new MyMetrics(this);
        mMetrics.startRunning();
    }

    @Override
    public void destroy()
    {
        if (mMetrics != null) {
            MyMetrics mm = mMetrics;
            mMetrics = null;
            mm.stopRunning();
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        final boolean doStats = mCfgUpdateMetrics;
        if (doStats) {
            synchronized (this) {
                ++mActiveRequests;
            }
        }

        try {
            // First, let's determine the method to use
            UUIDMethod method = determineMethod(req.getParameter("method"));
            String str = req.getParameter("count");
            int count = determineCount(str);
            String name = req.getParameter("name");
            checkParameters(method, count, name);
            int origCount = count;
            if (count > MAX_UUIDS_PER_REQUEST) {
                count = MAX_UUIDS_PER_REQUEST;
            }
            List<UUID> uuids = generateUuids(method, count, name);
            /* Could choose to add delay after calculating the response,
             * or before. Shouldn't matter too much.
             */
            if (REPLY_DELAY > 0) {
                try {
                    Thread.sleep(REPLY_DELAY);
                } catch (InterruptedException ie) {
                    System.err.println("Warning: delay sleep interrupted, skipping reply");
                    // Most likely server shutting down -- let's skip reply
                    return;
                }
            }
            writeResponse(resp, uuids, origCount);
        } catch (Throwable t) {
            reportProblem(resp, null, t);
        } finally {
            if (doStats) {
                synchronized (this) {
                    --mActiveRequests;
                    ++mRequestsServed;
                }
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        final boolean doStats = mCfgUpdateMetrics;
        if (doStats) {
            synchronized (this) {
                ++mActiveRequests;
            }
        }

        try {
            // Let's use the global Stax factory for the example
            InputStream in = req.getInputStream();
            SMInputCursor rootc = mSmInFactory.rootElementCursor(in);
            rootc.getNext(); // well-formed docs have single root

            // And root element should be "<request>"
            if (!rootc.hasLocalName("request")) {
                reportProblem(resp, "Root element not <request>, as expected, but <"+rootc.getLocalName()+">", null);
                return;
            }
            // Request has no attributes, but has 0+ methods (batches)
            SMInputCursor requests = rootc.childElementCursor();
            int totalReq = 0;

            List<UUID> uuids = new ArrayList<UUID>();
            while (requests.getNext() != null) {
                // Can ignore, or signal an error: let's do latter
                if (!requests.hasLocalName("generate-uuid")) {
                    reportProblem(resp, "Unrecognized element '"+requests.getLocalName()+"', expected <generate-uuid>", null);
                    return;
                }
                UUIDMethod method = determineMethod(requests.getAttrValue("method"));
                // note: could use typed accessor from here
                int count = determineCount(requests.getAttrValue("count"));
                String name = requests.getAttrValue("name");
                checkParameters(method, count, name);

                // Need to ensure we won't go beyond max per request
                totalReq += count;
                int max = MAX_UUIDS_PER_REQUEST - uuids.size();
                if (count > max) {
                    count = max;
                    if (count < 1) { // already reached max
                        continue; // continue to calc. max requested, still
                    }
                }
                uuids.addAll(generateUuids(method, count, name));
            }

            // All right; got them all, let's output
            writeResponse(resp, uuids, totalReq);
        } catch (Throwable t) {
            reportProblem(resp, "Failed to process POST request", t);
        } finally {
            if (doStats) {
                synchronized (this) {
                    --mActiveRequests;
                    ++mRequestsServed;
                }
            }
        }
    }

    void writeResponse(HttpServletResponse resp, List<UUID> uuids,
                       int totalRequested)
        throws IOException, XMLStreamException
    {
        resp.setContentType("text/xml");
        OutputStream out = resp.getOutputStream();
        SMOutputElement rootElem = writeDocWithRoot(out, "response");
        for (UUID uuid : uuids) {
            rootElem.addElement("uuid").addCharacters(uuid.toString());
        }
        /* If we had to truncate (caller asked for more uuids than we want
         * to return for a single call), let's add a comment indicating this:
         */
        if (totalRequested > uuids.size()) {
            rootElem.addComment("had to truncate "+(totalRequested - uuids.size())+" uuids; will only generate up to "+MAX_UUIDS_PER_REQUEST+" UUIDs per call");
        }
        // Need to close the root, to ensure all elements closed, flushed
        ((SMOutputDocument)rootElem.getParent()).closeRoot();
        out.flush();
    }

    SMOutputElement writeDocWithRoot(OutputStream out, String nonnsRootName)
        throws XMLStreamException
    {
        // this create method defaults to UTF-8
        SMOutputDocument doc = mSmOutFactory.createOutputDocument(out);

        /* Let's indent for debugging purposes: in production usually
         * shouldn't, to minimize message size. These settings give linefeed,
         * plus 2 spaces per level (initially just one char from the string,
         * linefeed, then 2 more chars per level
         */
        doc.setIndentation("\n                                    ", 1, 2);
        return doc.addElement(nonnsRootName);
    }

    void reportProblem(HttpServletResponse resp, String msg,
                       Throwable t)
        throws IOException
    {
        resp.setContentType("text/xml");
        OutputStream out = resp.getOutputStream();

        try {
            SMOutputElement rootElem = writeDocWithRoot(out, "error");
            
            // Let's customize a bit based on type of exception:
            if (t instanceof IllegalArgumentException) {
                // no need to pass exception, message is all we need
                msg = "Input argument problem: "+t.getMessage();
                t = null;
            } else if (t instanceof XMLStreamException) {
                msg = "Problem parsing xml request or writing response: "+t.getMessage();
            } else {
                if (msg == null) {
                    msg = "Problem processing request";
                }
            }
            rootElem.addElement("msg").addCharacters(msg);
            if (t != null) {
                SMOutputElement elem = rootElem.addElement("cause");
                elem.addAttribute("type", t.getClass().toString());
                String emsg = t.getMessage();
                if (emsg != null) {
                    elem.addCharacters(emsg);
                }
            }
            ((SMOutputDocument)rootElem.getParent()).closeRoot();
        } catch (XMLStreamException strex) {
            IOException ioe = new IOException(strex.getMessage());
            ioe.initCause(strex);
            throw ioe;
        }
    }
    
    List<UUID> generateUuids(UUIDMethod method, int count, String name)
    {
        UUIDGenerator gen = UUIDGenerator.getInstance();
        ArrayList<UUID> uuids = new ArrayList<UUID>(count);
        for (int i = 0; i < count; ++i) {
            UUID uuid;

            switch (method) {
            case RANDOM: // UUID using ~128 bits of randomness
                uuid = gen.generateRandomBasedUUID();
                break;
            case TIME: // UUID using time+location
                uuid = gen.generateTimeBasedUUID(mMacAddress);
                break;
            case NAME: // UUID computed from the given name
                /* Note: we do NOT use a context value, for simplicity --
                 * usually one should be used, and UUID class already
                 * specifies 4 suggested standard contexts
                 */
                uuid = gen.generateNameBasedUUID(null, name);
                break;
            default:
                throw new Error(); // never gets here
            }
            uuids.add(uuid);
        }
        return uuids;
    }

    // // // Input access, validation

    private int determineCount(String str)
    {
        if (str == null || str.length() == 0) { // missing? Defaults to 1
            return 1;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nex) {
            throw new IllegalArgumentException("Value of parameter 'count' not numeric ('"+str+"')");
        }
    }

    private UUIDMethod determineMethod(String str)
    {
        if (str == null || str.length() == 0) { // missing? Default to random
            return UUIDMethod.RANDOM;
        }
        try {
            return UUIDMethod.valueOf(str);
        } catch (IllegalArgumentException ex) {
            // Let's improve the message, make it more accurate
            throw new IllegalArgumentException("Unrecognized method '"+str+"', needs to be one of RANDOM, TIME (default) or NAME");
        }
    }

    private void checkParameters(UUIDMethod method, int count, String name)
    {
        if (method == UUIDMethod.NAME) {
            if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("Missing 'name' argument for UUID generation method NAME");
            }
        }
        if (count < 1) {
            throw new IllegalArgumentException("Illegal count value ("+count+"), has to be non-zero positive number");
        }
    }

    // // // Stats, metrics

    private int printMetrics(int prevReqs, long msecs)
    {
        int currReqs = mRequestsServed;

        /* Could (should) sync; but it's only for informational purposes
         * (human consumption) so let's just display as is
         */
        if (mCfgUpdateMetrics) {
            int done = currReqs - prevReqs;
            if (msecs == 0) {
                System.out.println("Warning: zero duration passed in");
                msecs = 1;
            }
            System.out.println("Requests active "+mActiveRequests+", served: "+done+" ("+(done * 1000 / msecs)+" per second)");
        }

        return currReqs;
    }

    final class MyMetrics
        implements Runnable
    {
        final static long METRICS_SLEEP = 2000L;

        final UuidServlet mParent;

        Thread mThread;

        boolean mRunning = false;

        int mLastCount = 0;

        public MyMetrics(UuidServlet parent)
        {
            mParent = parent;
        }

        public synchronized void startRunning()
        {
            if (!mRunning) {
                mThread = new Thread(this);
                mRunning = true;
                mThread.start();
            }
        }

        public synchronized void stopRunning()
        {
            if (mRunning) {
                //Thread t = mThread;
                mThread = null;
                mRunning = false;
                this.notify();
            }
        }

        public void run()
        {
            System.out.println("Starting metrics thread.");
            int prevReqs = 0;
            long prevTime = System.currentTimeMillis();
            while (true) {
                synchronized (this) {
                    if (!mRunning) {
                        break;
                    }
                }
                try {
                    Thread.sleep(METRICS_SLEEP);
                } catch (InterruptedException ie) {
                    System.out.println("Warning: interrupted sleep");
                    continue; // most likely stopping it for good
                }

                long now = System.currentTimeMillis();
                prevReqs = mParent.printMetrics(prevReqs, (now - prevTime));
                prevTime = now;
            }
            System.out.println("Stopping metrics thread.");
        }
    }
}
