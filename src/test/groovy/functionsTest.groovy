import org.junit.Before
import org.junit.Test

/**
  This class tests functions.groovy located in the scripts directory of this repository.
*/
class functionsTest extends GroovyTestCase {
    GroovyShell shell
    Binding binding
    File functions

    @Before protected void setUp() {
        binding = new Binding()
        shell = new GroovyShell(binding)
        functions = new File(this.getClass().getResource('/functions.groovy').getFile())
        shell.evaluate(functions)
    }
    @Test public void test_functions_foo() {
        assert 5 == binding.foo.with { it() }
    }
}
