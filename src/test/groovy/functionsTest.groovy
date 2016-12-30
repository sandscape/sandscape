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
    @Test public void test_functions_getObjectValue() {
        Map example = [key1: [subkey1: 'string']]
        assert 'string' == binding.getObjectValue.with { it(example, 'key1.subkey1', 'default') }
        assert 'default' == binding.getObjectValue.with { it(example, 'key2.subkey1', 'default') }
        assert 2 == binding.getObjectValue.with { it(example, 'key1.subkey1', 2) }
    }
    @Test public void test_functions_getObjectValue_false_bug() {
        Map example = [key1: [subkey1: 'false']]
        assert false == binding.getObjectValue.with { it(example, 'key1.subkey1', false) }
        assert false == binding.getObjectValue.with { it(example, 'key1.subkey1', true) }
    }
    @Test public void test_functions_getObjectValue_type_bug() {
        Map example = [key1: [subkey1: 'string'],key2: ["a", "b"]]
        assert 'default' == binding.getObjectValue.with { it(example, 'key1', 'default') }
        assert 'default' == binding.getObjectValue.with { it(example, 'key2', 'default') }
    }
}
