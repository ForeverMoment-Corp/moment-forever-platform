package $com.cherishx;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit jwt_exception_handler for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the jwt_exception_handler case
     *
     * @param testName name of the jwt_exception_handler case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
