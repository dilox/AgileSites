package $site;format="normalize"$.test;

import org.junit.Before;
import org.junit.Test;
import wcs.java.Env;
import wcs.java.util.TestElement;
import $site;format="normalize"$.element.$cselement$;

// this test must be run by AgileSites TestRunnerElement
public class $cselement$Test extends TestElement {

	$cselement$ it;
	
	@Before
	public void setUp() {
		it = new $cselement$();
	}

	@Test
	public void test() {
		parse(it.apply(env()));
		assertText("h1", "$cselement$");
	}

}